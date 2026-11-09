package com.example.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.SmartCleanResult
import com.example.model.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class FileRepository(private val context: Context) {

    private val rootStorageDir: File = Environment.getExternalStorageDirectory()

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val total = rootStorageDir.totalSpace
        val free = rootStorageDir.freeSpace
        val used = (total - free).coerceAtLeast(0)

        var imagesBytes = 0L
        var videosBytes = 0L
        var audioBytes = 0L
        var documentsBytes = 0L
        var appsBytes = 0L

        // Query MediaStore for fast aggregated category sizes
        runCatching {
            queryMediaSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)?.let { imagesBytes = it }
            queryMediaSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)?.let { videosBytes = it }
            queryMediaSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)?.let { audioBytes = it }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                queryMediaSize(MediaStore.Files.getContentUri("external"), MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_DOCUMENT)?.let {
                    documentsBytes = it
                }
            }
        }

        // Vault size
        val vaultDir = File(context.filesDir, "vault")
        val vaultBytes = vaultDir.listFiles()?.sumOf { it.length() } ?: 0L

        val knownBytes = imagesBytes + videosBytes + audioBytes + documentsBytes + appsBytes + vaultBytes
        val otherBytes = (used - knownBytes).coerceAtLeast(0)

        StorageStats(
            totalBytes = total,
            freeBytes = free,
            usedBytes = used,
            imagesBytes = imagesBytes,
            videosBytes = videosBytes,
            audioBytes = audioBytes,
            documentsBytes = documentsBytes,
            appsBytes = appsBytes,
            vaultBytes = vaultBytes,
            otherBytes = otherBytes
        )
    }

    private fun queryMediaSize(uri: Uri, selection: String? = null): Long? {
        val projection = arrayOf(MediaStore.MediaColumns.SIZE)
        var total = 0L
        return try {
            context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                while (cursor.moveToNext()) {
                    if (sizeCol != -1) {
                        total += cursor.getLong(sizeCol)
                    }
                }
                total
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRecentFiles(limit: Int = 10): List<FileItem> = withContext(Dispatchers.IO) {
        val recentItems = mutableListOf<FileItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC LIMIT $limit"

        val queryUris = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Downloads.EXTERNAL_CONTENT_URI else null
        ).filterNotNull()

        for (contentUri in queryUris) {
            runCatching {
                context.contentResolver.query(contentUri, projection, null, null, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = if (nameCol != -1) cursor.getString(nameCol) ?: "file" else "file"
                        val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                        val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                        val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "application/octet-stream" else ""
                        val modified = if (dateCol != -1) cursor.getLong(dateCol) * 1000L else System.currentTimeMillis()
                        val itemUri = ContentUris.withAppendedId(contentUri, id)

                        recentItems.add(
                            FileItem(
                                id = id.toString(),
                                name = name,
                                path = path,
                                size = size,
                                mimeType = mime,
                                lastModified = modified,
                                isDirectory = false,
                                category = FileCategory.fromExtension(name.substringAfterLast('.', "")),
                                uri = itemUri
                            )
                        )
                    }
                }
            }
        }

        // Sort by modified date descending
        recentItems.sortedByDescending { it.lastModified }.take(limit)
    }

    suspend fun getFiles(
        path: String,
        categoryFilter: FileCategory? = null,
        sortType: String = "NAME",
        sortAscending: Boolean = true
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val targetDir = if (path.isBlank() || path == "root") rootStorageDir else File(path)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            return@withContext emptyList()
        }

        val rawFiles = targetDir.listFiles() ?: emptyArray()

        val items = rawFiles.mapNotNull { file ->
            // Skip hidden files starting with . unless needed
            val isHidden = file.name.startsWith(".")
            val isDir = file.isDirectory
            val childrenCount = if (isDir) file.listFiles()?.size ?: 0 else 0
            val ext = file.extension
            val category = if (isDir) FileCategory.OTHER else FileCategory.fromExtension(ext)

            if (categoryFilter != null && !isDir && category != categoryFilter) {
                return@mapNotNull null
            }

            FileItem(
                id = file.absolutePath,
                name = file.name,
                path = file.absolutePath,
                size = if (isDir) 0L else file.length(),
                mimeType = if (isDir) "directory" else getMimeType(file.name),
                lastModified = file.lastModified(),
                isDirectory = isDir,
                isHidden = isHidden,
                category = category,
                childrenCount = childrenCount
            )
        }

        // Sort: Always list directories first, then apply sort criteria
        items.sortedWith(Comparator { a, b ->
            if (a.isDirectory && !b.isDirectory) return@Comparator -1
            if (!a.isDirectory && b.isDirectory) return@Comparator 1

            val comparison = when (sortType) {
                "DATE" -> a.lastModified.compareTo(b.lastModified)
                "SIZE" -> a.size.compareTo(b.size)
                "TYPE" -> a.extension.compareTo(b.extension, ignoreCase = true)
                else -> a.name.compareTo(b.name, ignoreCase = true)
            }
            if (sortAscending) comparison else -comparison
        })
    }

    suspend fun searchFiles(query: String): List<FileItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<FileItem>()
        val searchFolders = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            rootStorageDir
        ).distinct().filter { it.exists() }

        for (dir in searchFolders) {
            searchRecursive(dir, query.lowercase(Locale.getDefault()), results, maxDepth = 3)
            if (results.size >= 50) break
        }
        results.take(50)
    }

    private fun searchRecursive(dir: File, query: String, results: MutableList<FileItem>, maxDepth: Int) {
        if (maxDepth <= 0 || results.size >= 50) return
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.name.startsWith(".")) continue
            if (f.name.lowercase(Locale.getDefault()).contains(query)) {
                results.add(
                    FileItem(
                        id = f.absolutePath,
                        name = f.name,
                        path = f.absolutePath,
                        size = if (f.isDirectory) 0 else f.length(),
                        mimeType = if (f.isDirectory) "directory" else getMimeType(f.name),
                        lastModified = f.lastModified(),
                        isDirectory = f.isDirectory,
                        category = if (f.isDirectory) FileCategory.OTHER else FileCategory.fromExtension(f.extension),
                        childrenCount = if (f.isDirectory) f.listFiles()?.size ?: 0 else 0
                    )
                )
            }
            if (f.isDirectory) {
                searchRecursive(f, query, results, maxDepth - 1)
            }
        }
    }

    suspend fun getSmartCleanData(): SmartCleanResult = withContext(Dispatchers.IO) {
        val largeFiles = mutableListOf<FileItem>()
        val duplicates = mutableListOf<FileItem>()
        val cacheFiles = mutableListOf<FileItem>()
        val tempFiles = mutableListOf<FileItem>()

        val sizeMap = mutableMapOf<Long, MutableList<FileItem>>()

        // Scan common user storage directories
        val scanDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            context.cacheDir,
            context.externalCacheDir
        ).filterNotNull().filter { it.exists() }

        for (dir in scanDirs) {
            dir.walkTopDown().maxDepth(3).forEach { file ->
                if (!file.isDirectory && file.exists()) {
                    val length = file.length()
                    val item = FileItem(
                        id = file.absolutePath,
                        name = file.name,
                        path = file.absolutePath,
                        size = length,
                        mimeType = getMimeType(file.name),
                        lastModified = file.lastModified(),
                        isDirectory = false,
                        category = FileCategory.fromExtension(file.extension)
                    )

                    // Large files > 50MB
                    if (length > 50 * 1024 * 1024) {
                        largeFiles.add(item)
                    }

                    // Cache / Temp detection
                    val lower = file.name.lowercase()
                    if (lower.endsWith(".tmp") || lower.endsWith(".log") || lower.endsWith(".cache") || file.path.contains("cache")) {
                        cacheFiles.add(item)
                    }

                    // Potential duplicates check (match on size > 1MB)
                    if (length > 1024 * 1024) {
                        val list = sizeMap.getOrPut(length) { mutableListOf() }
                        list.add(item)
                    }
                }
            }
        }

        // Identify duplicates
        for ((_, list) in sizeMap) {
            if (list.size > 1) {
                duplicates.addAll(list.drop(1)) // Keep first, mark others as duplicates
            }
        }

        val totalReclaimable = duplicates.sumOf { it.size } +
                cacheFiles.sumOf { it.size } +
                tempFiles.sumOf { it.size }

        SmartCleanResult(
            duplicateFiles = duplicates.take(15),
            largeFiles = largeFiles.take(15),
            cacheFiles = cacheFiles.take(20),
            temporaryFiles = tempFiles.take(20),
            totalReclaimableBytes = totalReclaimable
        )
    }

    suspend fun cleanCacheAndTempFiles(filesToClean: List<FileItem>): Long = withContext(Dispatchers.IO) {
        var reclaimed = 0L
        for (item in filesToClean) {
            val f = File(item.path)
            if (f.exists()) {
                val len = f.length()
                if (f.delete()) {
                    reclaimed += len
                }
            }
        }
        reclaimed
    }

    suspend fun createFolder(parentPath: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val target = File(parentPath, name)
        if (!target.exists()) target.mkdirs() else false
    }

    suspend fun deleteFile(item: FileItem): Boolean = withContext(Dispatchers.IO) {
        val file = File(item.path)
        if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    suspend fun moveFile(item: FileItem, targetDirPath: String): Boolean = withContext(Dispatchers.IO) {
        val src = File(item.path)
        val dst = File(targetDirPath, item.name)
        if (src.exists() && !dst.exists()) {
            src.renameTo(dst)
        } else false
    }

    suspend fun renameFile(item: FileItem, newName: String): Boolean = withContext(Dispatchers.IO) {
        val src = File(item.path)
        val dst = File(src.parentFile, newName)
        if (src.exists() && !dst.exists()) {
            src.renameTo(dst)
        } else false
    }

    fun getBreadcrumbs(path: String): List<Pair<String, String>> {
        val rootPath = rootStorageDir.absolutePath
        if (path.isBlank() || path == "root" || path == rootPath) {
            return listOf("Internal Storage" to rootPath)
        }

        val breadcrumbs = mutableListOf<Pair<String, String>>()
        var current: File? = File(path)

        while (current != null && current.absolutePath.startsWith(rootPath)) {
            val name = if (current.absolutePath == rootPath) "Internal Storage" else current.name
            breadcrumbs.add(0, name to current.absolutePath)
            if (current.absolutePath == rootPath) break
            current = current.parentFile
        }

        return if (breadcrumbs.isEmpty()) listOf("Internal Storage" to rootPath) else breadcrumbs
    }

    fun getRootPath(): String = rootStorageDir.absolutePath

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }
}
