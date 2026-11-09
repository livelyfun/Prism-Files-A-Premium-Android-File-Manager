package com.example.model

data class SmartCleanResult(
    val duplicateFiles: List<FileItem> = emptyList(),
    val largeFiles: List<FileItem> = emptyList(),
    val cacheFiles: List<FileItem> = emptyList(),
    val temporaryFiles: List<FileItem> = emptyList(),
    val totalReclaimableBytes: Long = 0
) {
    val formattedReclaimable: String
        get() = StorageStats.formatBytes(totalReclaimableBytes)
}
