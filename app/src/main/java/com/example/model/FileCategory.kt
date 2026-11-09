package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class FileCategory(
    val title: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    IMAGES(
        title = "Images",
        icon = Icons.Default.Image,
        primaryColor = Color(0xFF38EF7D),
        secondaryColor = Color(0xFF11998E)
    ),
    VIDEOS(
        title = "Videos",
        icon = Icons.Default.VideoFile,
        primaryColor = Color(0xFFFF5252),
        secondaryColor = Color(0xFFFF1744)
    ),
    AUDIO(
        title = "Audio",
        icon = Icons.Default.AudioFile,
        primaryColor = Color(0xFFFF9100),
        secondaryColor = Color(0xFFFF6D00)
    ),
    DOCUMENTS(
        title = "Documents",
        icon = Icons.Default.Description,
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFF00B0FF)
    ),
    DOWNLOADS(
        title = "Downloads",
        icon = Icons.Default.Download,
        primaryColor = Color(0xFF7C4DFF),
        secondaryColor = Color(0xFF651FFF)
    ),
    ARCHIVES(
        title = "Archives",
        icon = Icons.Default.Archive,
        primaryColor = Color(0xFFFFD600),
        secondaryColor = Color(0xFFFFAB00)
    ),
    APPS(
        title = "Apps",
        icon = Icons.Default.Widgets,
        primaryColor = Color(0xFF00BFA5),
        secondaryColor = Color(0xFF00897B)
    ),
    VAULT(
        title = "Vault",
        icon = Icons.Default.Lock,
        primaryColor = Color(0xFFFF4081),
        secondaryColor = Color(0xFFD500F9)
    ),
    OTHER(
        title = "Other",
        icon = Icons.Default.Folder,
        primaryColor = Color(0xFF90A4AE),
        secondaryColor = Color(0xFF607D8B)
    );

    companion object {
        fun fromExtension(ext: String): FileCategory {
            val lower = ext.lowercase().trimStart('.')
            return when (lower) {
                "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "svg" -> IMAGES
                "mp4", "mkv", "mov", "avi", "webm", "flv", "3gp", "wmv" -> VIDEOS
                "mp3", "wav", "m4a", "flac", "aac", "ogg", "opus" -> AUDIO
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "json" -> DOCUMENTS
                "zip", "rar", "7z", "tar", "gz" -> ARCHIVES
                "apk", "aab", "xapk" -> APPS
                "pvf" -> VAULT
                else -> OTHER
            }
        }
    }
}
