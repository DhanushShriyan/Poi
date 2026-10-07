package com.poi.core.poster

/** Uses OCR line geometry/confidence instead of promoting the first watermark to an event title. */
internal object PosterOcrLayout {
    private data class Line(val text: String, val height: Int, val confidence: Int)
    fun orderedText(hocr: String, fallback: String): String {
        val lineStarts = Regex("<span\\b[^>]*class=['\"]ocr_line['\"][^>]*>").findAll(hocr).toList()
        val words = Regex("<span\\b[^>]*class=['\"]ocrx_word['\"][^>]*>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
        val bbox = Regex("bbox (\\d+) (\\d+) (\\d+) (\\d+)")
        val confidence = Regex("x_wconf (\\d+)")
        val lines = lineStarts.mapIndexedNotNull { index, start ->
            val block = hocr.substring(start.range.first, lineStarts.getOrNull(index + 1)?.range?.first ?: hocr.length)
            val bounds = bbox.find(start.value)?.groupValues ?: return@mapIndexedNotNull null
            val recognized = words.findAll(block).mapNotNull { word ->
                val score = confidence.find(word.value)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val value = word.groupValues[1].replace(Regex("<[^>]+>"), "")
                    .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
                val connector = value.trim() in setOf("&", "-", "–", ",", ":")
                if (!connector && (score < 35 || value.none(Char::isLetterOrDigit))) null else value to score
            }.toList()
            if (recognized.isEmpty()) null else Line(recognized.joinToString(" ") { it.first }, bounds[4].toInt() - bounds[2].toInt(), recognized.sumOf { it.second } / recognized.size)
        }
        if (lines.isEmpty()) return fallback
        val candidate = lines.filter { it.confidence >= 50 && it.text.count(Char::isLetter) >= 4 && it.text.length <= 100 }
            .maxByOrNull { it.height * it.confidence }
        // The parser still rejects promotions, venue lines and dates. Do not manufacture text.
        return (listOfNotNull(candidate?.text) + lines.map { it.text }).distinct().joinToString("\n")
    }
}
