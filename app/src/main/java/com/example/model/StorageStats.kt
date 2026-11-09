package com.example.model

import java.util.Locale

data class StorageStats(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val imagesBytes: Long = 0,
    val videosBytes: Long = 0,
    val audioBytes: Long = 0,
    val documentsBytes: Long = 0,
    val appsBytes: Long = 0,
    val vaultBytes: Long = 0,
    val otherBytes: Long = 0
) {
    val usedPercent: Float
        get() = if (totalBytes > 0) ((usedBytes.toFloat() / totalBytes) * 100f).coerceIn(0f, 100f) else 0f

    val formattedTotal: String get() = formatBytes(totalBytes)
    val formattedUsed: String get() = formatBytes(usedBytes)
    val formattedFree: String get() = formatBytes(freeBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 GB"
            val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 1.0) {
                String.format(Locale.US, "%.1f GB", gb)
            } else {
                val mb = bytes.toDouble() / (1024.0 * 1024.0)
                String.format(Locale.US, "%.1f MB", mb)
            }
        }
    }
}
