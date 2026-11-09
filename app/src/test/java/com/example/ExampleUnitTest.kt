package com.example

import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.StorageStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun fileCategory_mapping() {
        assertEquals(FileCategory.IMAGES, FileCategory.fromExtension("jpg"))
        assertEquals(FileCategory.IMAGES, FileCategory.fromExtension("PNG"))
        assertEquals(FileCategory.VIDEOS, FileCategory.fromExtension("mp4"))
        assertEquals(FileCategory.AUDIO, FileCategory.fromExtension("mp3"))
        assertEquals(FileCategory.DOCUMENTS, FileCategory.fromExtension("pdf"))
        assertEquals(FileCategory.ARCHIVES, FileCategory.fromExtension("zip"))
        assertEquals(FileCategory.APPS, FileCategory.fromExtension("apk"))
        assertEquals(FileCategory.VAULT, FileCategory.fromExtension("pvf"))
    }

    @Test
    fun fileItem_formatting() {
        val item = FileItem(
            id = "test-1",
            name = "document.pdf",
            path = "/sdcard/document.pdf",
            size = 1048576L * 5, // 5 MB
            mimeType = "application/pdf",
            lastModified = 1700000000000L,
            isDirectory = false,
            category = FileCategory.DOCUMENTS
        )

        assertEquals("pdf", item.extension)
        assertEquals("5.0 MB", item.formattedSize)
        assertTrue(item.formattedDate.isNotBlank())
    }

    @Test
    fun storageStats_calculations() {
        val stats = StorageStats(
            totalBytes = 107374182400L, // 100 GB
            freeBytes = 53687091200L,   // 50 GB
            usedBytes = 53687091200L
        )

        assertEquals("100.0 GB", stats.formattedTotal)
        assertEquals("50.0 GB", stats.formattedFree)
        assertEquals("50.0 GB", stats.formattedUsed)
        assertEquals(50f, stats.usedPercent, 0.1f)
    }
}
