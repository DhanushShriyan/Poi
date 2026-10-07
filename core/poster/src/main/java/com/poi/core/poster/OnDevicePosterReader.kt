package com.poi.core.poster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Images and recognized text stay on device. Bundled models mean no scan-time downloads. */
class OnDevicePosterReader(context: Context) {
    private val appContext = context.applicationContext

    suspend fun read(uri: Uri, language: PosterLanguage, rotation: Int = 0): PosterScan = withContext(Dispatchers.IO) {
        recognitionLock.withLock {
            currentCoroutineContext().ensureActive()
            val dataRoot = File(appContext.filesDir, "poster-ocr-v1")
            val data = File(dataRoot, "tessdata").apply { mkdirs() }
            language.models.split('+').forEach { code ->
                val file = File(data, "$code.traineddata")
                if (!file.exists()) {
                    val temporary = File(data, "$code.tmp")
                    try {
                        appContext.assets.open("poster-ocr/$code.traineddata").use { input ->
                            temporary.outputStream().use(input::copyTo)
                        }
                        check(temporary.renameTo(file)) { "Could not prepare the language reader." }
                    } finally { temporary.delete() }
                }
            }
            val bitmap = decode(uri, rotation)
            val engine = TessBaseAPI()
            try {
                check(engine.init(dataRoot.absolutePath, language.models, TessBaseAPI.OEM_LSTM_ONLY)) {
                    "The language reader could not start. Please retry."
                }
                engine.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT)
                engine.setImage(bitmap)
                val scanStarted = android.os.SystemClock.elapsedRealtime()
                var (text, confidence) = recognize(engine, 60_000)
                currentCoroutineContext().ensureActive()
                if (text.count(Char::isLetterOrDigit) < 30 || confidence < 40) {
                    engine.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                    engine.setImage(bitmap)
                    val remaining = 60_000 - (android.os.SystemClock.elapsedRealtime() - scanStarted)
                    val (alternative, alternativeConfidence) = recognize(engine, remaining.coerceAtLeast(1))
                    if (alternative.count(Char::isLetterOrDigit) > text.count(Char::isLetterOrDigit) && alternativeConfidence >= confidence - 5) {
                        text = alternative; confidence = alternativeConfidence
                    }
                }
                currentCoroutineContext().ensureActive()
                require(text.count(Char::isLetterOrDigit) >= 8) {
                    "Not enough readable text. Try a sharper crop, rotate the poster, or select its language."
                }
                PosterScan(PosterParser.parse(text), confidence < 60)
            } finally {
                engine.recycle()
                bitmap.recycle()
            }
        }
    }

    private suspend fun recognize(engine: TessBaseAPI, remainingMillis: Long): Pair<String, Int> = coroutineScope {
        val finished = AtomicBoolean(false)
        val timedOut = AtomicBoolean(false)
        val watchdog = launch(Dispatchers.IO) {
            try {
                delay(remainingMillis)
                if (!finished.get()) { timedOut.set(true); engine.stop() }
            } finally {
                if (!finished.get()) engine.stop()
            }
        }
        try {
            // hOCR starts interruptible recognition; UTF8 then reads the existing result.
            engine.getHOCRText(0)
            currentCoroutineContext().ensureActive()
            check(!timedOut.get()) { "Reading took too long. Crop the poster or choose a specific language, then retry." }
            engine.getUTF8Text().orEmpty() to engine.meanConfidence()
        } finally {
            finished.set(true)
            withContext(NonCancellable) { watchdog.cancelAndJoin() }
        }
    }

    private fun decode(uri: Uri, rotation: Int): Bitmap {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth in 1..40_000 && bounds.outHeight in 1..40_000) { "Choose a supported photo or poster image." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2600) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888
            })
        } ?: error("The image could not be opened. Choose it again.")
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        }.getOrNull()
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        }
        matrix.postRotate(rotation.toFloat())
        val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (oriented !== decoded) decoded.recycle()
        // Flatten transparent posters on white; retain colour for the OCR thresholding engine.
        val output = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
        Canvas(output).apply { drawColor(Color.WHITE); drawBitmap(oriented, 0f, 0f, null) }
        oriented.recycle()
        return output
    }

    private companion object { val recognitionLock = Mutex() }
}
