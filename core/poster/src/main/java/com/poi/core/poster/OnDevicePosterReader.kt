package com.poi.core.poster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
            var engine = TessBaseAPI()
            try {
                check(engine.init(dataRoot.absolutePath, language.models, TessBaseAPI.OEM_LSTM_ONLY)) {
                    "The language reader could not start. Please retry."
                }
                engine.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT)
                engine.setImage(bitmap)
                val scanStarted = android.os.SystemClock.elapsedRealtime()
                var (text, confidence) = recognize(engine, 60_000)
                currentCoroutineContext().ensureActive()
                if (language == PosterLanguage.AUTO || text.count(Char::isLetterOrDigit) < 30 || confidence < 40) {
                    // A Latin-only pass avoids competing scripts corrupting English summary headers.
                    if (language == PosterLanguage.AUTO) {
                        engine.recycle()
                        engine = TessBaseAPI()
                        check(engine.init(dataRoot.absolutePath, "eng", TessBaseAPI.OEM_LSTM_ONLY)) { "The English reader could not start." }
                    }
                    engine.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT)
                    engine.setImage(bitmap)
                    val remaining = 60_000 - (android.os.SystemClock.elapsedRealtime() - scanStarted)
                    if (remaining > 5_000) {
                        val alternative = runCatching { recognize(engine, remaining) }.getOrElse {
                            currentCoroutineContext().ensureActive()
                            if (text.count(Char::isLetterOrDigit) < 8) throw it
                            "" to 0
                        }
                        val englishHeader = Regex("kambala|concert|fan park|circus|festival|workshop", RegexOption.IGNORE_CASE).containsMatchIn(alternative.first)
                        if (language == PosterLanguage.AUTO && alternative.second >= 45 && englishHeader) {
                            text = (alternative.first.lines() + text.lines()).distinct().joinToString("\n")
                            confidence = maxOf(confidence, alternative.second)
                        } else if (language != PosterLanguage.AUTO && alternative.first.count(Char::isLetterOrDigit) > text.count(Char::isLetterOrDigit) && alternative.second >= confidence - 5) {
                            text = alternative.first; confidence = alternative.second
                        }
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
            val hocr = engine.getHOCRText(0).orEmpty()
            currentCoroutineContext().ensureActive()
            check(!timedOut.get()) { "Reading took too long. Crop the poster or choose a specific language, then retry." }
            PosterOcrLayout.orderedText(hocr, engine.getUTF8Text().orEmpty()) to engine.meanConfidence()
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
        val scale = (1600f / maxOf(oriented.width, oriented.height)).coerceAtLeast(1f)
        val sized = if (scale > 1f) Bitmap.createScaledBitmap(oriented, (oriented.width * scale).toInt(), (oriented.height * scale).toInt(), true) else oriented
        if (sized !== oriented) oriented.recycle()
        // Normalize dark/gold and bright-colour posters to high-contrast grayscale.
        var luminance = 0L; var samples = 0
        for (y in 0 until sized.height step 20) for (x in 0 until sized.width step 20) {
            val pixel = sized.getPixel(x, y)
            luminance += (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000
            samples++
        }
        val dark = samples > 0 && luminance / samples < 110
        val contrast = if (dark) -1.4f else 1.4f
        val offset = if (dark) 307f else -51f
        val matrixColour = ColorMatrix(floatArrayOf(
            .299f * contrast, .587f * contrast, .114f * contrast, 0f, offset,
            .299f * contrast, .587f * contrast, .114f * contrast, 0f, offset,
            .299f * contrast, .587f * contrast, .114f * contrast, 0f, offset,
            0f, 0f, 0f, 1f, 0f,
        ))
        val output = Bitmap.createBitmap(sized.width, sized.height, Bitmap.Config.ARGB_8888)
        Canvas(output).apply { drawColor(Color.WHITE); drawBitmap(sized, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(matrixColour) }) }
        sized.recycle()
        return output
    }

    private companion object { val recognitionLock = Mutex() }
}
