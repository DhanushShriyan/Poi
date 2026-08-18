package com.poi.core.update

data class AppUpdate(
    val versionName: String,
    val downloadUrl: String,
    val notes: String,
)

object VersionComparator {
    fun isNewer(remoteVersion: String, currentVersion: String): Boolean {
        val remoteParts = numericParts(remoteVersion)
        val currentParts = numericParts(currentVersion)
        val partCount = maxOf(remoteParts.size, currentParts.size)

        repeat(partCount) { index ->
            val remote = remoteParts.getOrElse(index) { 0 }
            val current = currentParts.getOrElse(index) { 0 }
            if (remote != current) return remote > current
        }
        return false
    }

    private fun numericParts(version: String): List<Int> =
        version
            .trim()
            .removePrefix("v")
            .substringBefore('-')
            .split('.')
            .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
