package com.poi.core.poster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import com.poi.core.model.EventCategory
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

/** Only calls Poi's authenticated backend; the Gemini credential never reaches this client. */
class CloudPosterReader(
    context: Context,
    private val projectUrl: String,
    private val publishableKey: String,
    private val accessToken: () -> String?,
) {
    private val context = context.applicationContext
    suspend fun read(uri: Uri, rotation: Int): PosterScan = withContext(Dispatchers.IO) { coroutineScope {
        val token = accessToken()?.takeIf { it.isNotBlank() } ?: error("Sign in to use AI reading.")
        val image = prepareImage(uri, rotation)
        currentCoroutineContext().ensureActive()
        val connection = URL("$projectUrl/functions/v1/read-poster").openConnection() as HttpURLConnection
        val finished = AtomicBoolean(false)
        val watchdog = launch(Dispatchers.IO) {
            try { delay(85_000) } finally { if (!finished.get()) connection.disconnect() }
        }
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 75_000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("apikey", publishableKey)
            connection.setRequestProperty("Authorization", "Bearer $token")
            val body = JSONObject().put("mimeType", "image/jpeg").put("image", Base64.encodeToString(image, Base64.NO_WRAP)).put("consent", true).toString().toByteArray()
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
            val code = connection.responseCode
            val input = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = input?.use { stream ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (out.size() <= 65_536) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    out.write(buffer, 0, count)
                }
                check(out.size() <= 65_536) { "Unexpected reader response. Please retry." }
                out.toString("UTF-8")
            }.orEmpty()
            currentCoroutineContext().ensureActive()
            check(text.length <= 65_536) { "Unexpected reader response. Please retry." }
            if (code !in 200..299) error(runCatching { JSONObject(text).optString("error") }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: if (code == 401) "Please sign in again to use AI reading." else "AI reading is unavailable. Try offline reading.")
            val json = JSONObject(text)
            val dates = json.getJSONArray("dates").let { array -> (0 until array.length()).map { index ->
                val d = array.getJSONObject(index)
                PosterDateSuggestion(d.getInt("month"), d.getInt("day"), if (d.isNull("year")) null else d.getInt("year"))
            } }
            val times = json.getJSONArray("startTimes").let { a -> (0 until a.length()).map(a::getString) }
            val warnings = json.getJSONArray("warnings").let { a -> (0 until a.length()).map(a::getString) }
            val title = json.optString("title")
            val original = json.optString("originalTitle")
            val category = PosterParser.parse(title).category.takeUnless { it == EventCategory.ALL } ?: EventCategory.COMMUNITY
            val range = json.optString("dateRelationship") == "range"
            PosterScan(PosterDraft(
                sourceText = json.optString("evidence"), title = title, summary = json.optString("summary"),
                venue = json.optString("venue"), address = json.optString("locality"), category = category,
                date = dates.singleOrNull()?.resolve(), time = times.singleOrNull(), endDate = null,
                endTime = if (json.isNull("endTime")) null else json.optString("endTime"),
                warnings = warnings + if (original.isNotBlank() && original != title) listOf("Original heading: $original") else emptyList(),
                dateSuggestions = dates, timeSuggestions = times, continuousDateRange = range,
            ), weakRecognition = false)
        } catch (failure: java.io.IOException) {
            currentCoroutineContext().ensureActive()
            error("Could not reach the AI reader. Check your connection, retry, or use offline reading.")
        } finally {
            finished.set(true)
            withContext(NonCancellable) { watchdog.cancelAndJoin() }
            connection.disconnect()
        }
    } }

    private fun prepareImage(uri: Uri, rotation: Int): ByteArray {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth in 1..40_000 && bounds.outHeight in 1..40_000) { "Choose a supported poster image." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2200) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: error("The poster could not be opened.")
        val orientation = runCatching { resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) } }.getOrNull()
        val matrix = Matrix()
        when (orientation) {
            2 -> matrix.setScale(-1f, 1f)
            3 -> matrix.setRotate(180f)
            4 -> matrix.setScale(1f, -1f)
            5 -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            6 -> matrix.setRotate(90f)
            7 -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
            8 -> matrix.setRotate(-90f)
        }
        matrix.postRotate(rotation.toFloat())
        val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (oriented !== decoded) decoded.recycle()
        val flat = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
        Canvas(flat).apply { drawColor(Color.WHITE); drawBitmap(oriented, 0f, 0f, null) }
        oriented.recycle()
        return try {
            val out = ByteArrayOutputStream()
            flat.compress(Bitmap.CompressFormat.JPEG, 88, out)
            require(out.size() <= 2_100_000) { "Poster is too large. Crop it and try again." }
            out.toByteArray()
        } finally { flat.recycle() }
    }
}
