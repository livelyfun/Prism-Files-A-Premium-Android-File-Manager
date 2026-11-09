package com.example.model

import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String,
    val lastModified: Long,
    val isDirectory: Boolean,
    val isHidden: Boolean = false,
    val isVaultItem: Boolean = false,
    val category: FileCategory = FileCategory.OTHER,
    val uri: Uri? = null,
    val childrenCount: Int = 0,
    val originalVaultName: String? = null
) {
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "")

    val formattedSize: String
        get() {
            if (isDirectory) return "$childrenCount items"
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            if (digitGroups >= units.size) digitGroups = units.size - 1
            val value = size / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    fun toFile(): File = File(path)
}
