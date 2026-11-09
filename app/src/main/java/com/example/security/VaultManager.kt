package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.model.FileCategory
import com.example.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class VaultManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("prism_vault_prefs", Context.MODE_PRIVATE)

    private val vaultDir: File = File(context.filesDir, "vault").apply {
        if (!exists()) mkdirs()
    }

    private val cacheDir: File = File(context.cacheDir, "vault_cache").apply {
        if (!exists()) mkdirs()
    }

    init {
        ensureNoMedia()
    }

    private fun ensureNoMedia() {
        val noMedia = File(vaultDir, ".nomedia")
        if (!noMedia.exists()) {
            runCatching { noMedia.createNewFile() }
        }
        val cacheNoMedia = File(cacheDir, ".nomedia")
        if (!cacheNoMedia.exists()) {
            runCatching { cacheNoMedia.createNewFile() }
        }
    }

    // --- Key Management via AndroidKeyStore (Hardware-backed AES-256) ---
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private fun getOrCreateMasterKey(): SecretKey {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    // --- PIN & Authentication State ---
    val isPinSet: Boolean
        get() = prefs.contains(KEY_PIN_HASH)

    var isPanicModeActive: Boolean
        get() = prefs.getBoolean(KEY_PANIC_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_PANIC_MODE, value).apply()

    var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        private set(value) = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var autoLockOnExit: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LOCK, value).apply()

    fun setupPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        val saltHex = bytesToHex(salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, saltHex)
            .putString(KEY_PIN_HASH, hash)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putBoolean(KEY_PANIC_MODE, false)
            .apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        if (isPanicModeActive) return false
        val saltHex = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = hexToBytes(saltHex)
        val computedHash = hashPin(pin, salt)

        if (computedHash == savedHash) {
            failedAttempts = 0
            return true
        } else {
            val newFailed = failedAttempts + 1
            failedAttempts = newFailed
            if (newFailed >= MAX_FAILED_ATTEMPTS) {
                isPanicModeActive = true
            }
            return false
        }
    }

    fun resetPanicMode() {
        failedAttempts = 0
        isPanicModeActive = false
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val digest = md.digest(pin.toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(digest)
    }

    // --- File Encryption & Decryption (AES-256-GCM) ---
    suspend fun encryptAndImportFile(file: File): Result<FileItem> = withContext(Dispatchers.IO) {
        runCatching {
            ensureNoMedia()
            if (!file.exists()) throw IllegalArgumentException("File does not exist: ${file.path}")

            val secretKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv

            // Prepare metadata JSON
            val meta = JSONObject().apply {
                put("orig_name", file.name)
                put("orig_path", file.absolutePath)
                put("size", file.length())
                put("last_modified", file.lastModified())
                put("mime_type", getMimeType(file.name))
            }.toString().toByteArray(StandardCharsets.UTF_8)

            val metaLengthBytes = ByteBuffer.allocate(4).putInt(meta.size).array()

            val randomId = UUID.randomUUID().toString()
            val destinationPvf = File(vaultDir, "$randomId.pvf")

            FileOutputStream(destinationPvf).use { fos ->
                // Header: MAGIC (4 bytes) + IV Length (1 byte) + IV (12 bytes) + Meta Length (4 bytes) + Meta + Encrypted File
                fos.write(MAGIC)
                fos.write(iv.size)
                fos.write(iv)
                fos.write(metaLengthBytes)
                fos.write(meta)

                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val outputChunk = cipher.update(buffer, 0, bytesRead)
                        if (outputChunk != null && outputChunk.isNotEmpty()) {
                            fos.write(outputChunk)
                        }
                    }
                    val finalChunk = cipher.doFinal()
                    if (finalChunk != null && finalChunk.isNotEmpty()) {
                        fos.write(finalChunk)
                    }
                }
            }

            // Remove original file after encryption
            val originalName = file.name
            val originalLength = file.length()
            val originalModified = file.lastModified()
            val category = FileCategory.fromExtension(file.extension)
            runCatching { file.delete() }

            FileItem(
                id = randomId,
                name = originalName,
                path = destinationPvf.absolutePath,
                size = originalLength,
                mimeType = getMimeType(originalName),
                lastModified = originalModified,
                isDirectory = false,
                isVaultItem = true,
                category = category,
                originalVaultName = originalName
            )
        }
    }

    suspend fun decryptToTempCache(vaultItem: FileItem): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            ensureNoMedia()
            val pvfFile = File(vaultItem.path)
            if (!pvfFile.exists()) throw IllegalArgumentException("Vault file missing: ${vaultItem.path}")

            FileInputStream(pvfFile).use { fis ->
                val magic = ByteArray(4)
                fis.read(magic)
                if (!magic.contentEquals(MAGIC)) {
                    throw IllegalStateException("Invalid PVF file header")
                }

                val ivSize = fis.read()
                val iv = ByteArray(ivSize)
                fis.read(iv)

                val metaLengthBuf = ByteArray(4)
                fis.read(metaLengthBuf)
                val metaLength = ByteBuffer.wrap(metaLengthBuf).int
                val metaBytes = ByteArray(metaLength)
                fis.read(metaBytes)

                val meta = JSONObject(String(metaBytes, StandardCharsets.UTF_8))
                val origName = meta.optString("orig_name", "decrypted_file")
                val ext = origName.substringAfterLast('.', "tmp")

                val secretKey = getOrCreateMasterKey()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                val tempFile = File(cacheDir, "pvf_${vaultItem.id}_$origName").apply {
                    deleteOnExit()
                }

                FileOutputStream(tempFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val outputChunk = cipher.update(buffer, 0, bytesRead)
                        if (outputChunk != null && outputChunk.isNotEmpty()) {
                            fos.write(outputChunk)
                        }
                    }
                    val finalChunk = cipher.doFinal()
                    if (finalChunk != null && finalChunk.isNotEmpty()) {
                        fos.write(finalChunk)
                    }
                }

                tempFile
            }
        }
    }

    suspend fun exportAndUnhide(vaultItem: FileItem, targetDirectory: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val decryptedTemp = decryptToTempCache(vaultItem).getOrThrow()
            val targetFile = File(targetDirectory, vaultItem.name)
            decryptedTemp.copyTo(targetFile, overwrite = true)
            decryptedTemp.delete()
            File(vaultItem.path).delete()
            targetFile
        }
    }

    suspend fun deleteVaultItem(vaultItem: FileItem): Boolean = withContext(Dispatchers.IO) {
        val file = File(vaultItem.path)
        file.delete()
    }

    suspend fun getVaultFiles(): List<FileItem> = withContext(Dispatchers.IO) {
        val files = vaultDir.listFiles { _, name -> name.endsWith(".pvf") } ?: emptyArray()
        files.mapNotNull { pvf ->
            readVaultMetadata(pvf)
        }
    }

    private fun readVaultMetadata(pvf: File): FileItem? {
        return try {
            FileInputStream(pvf).use { fis ->
                val magic = ByteArray(4)
                fis.read(magic)
                if (!magic.contentEquals(MAGIC)) return null

                val ivSize = fis.read()
                fis.skip(ivSize.toLong())

                val metaLengthBuf = ByteArray(4)
                fis.read(metaLengthBuf)
                val metaLength = ByteBuffer.wrap(metaLengthBuf).int
                val metaBytes = ByteArray(metaLength)
                fis.read(metaBytes)

                val meta = JSONObject(String(metaBytes, StandardCharsets.UTF_8))
                val origName = meta.getString("orig_name")
                val origSize = meta.getLong("size")
                val origModified = meta.getLong("last_modified")
                val origMime = meta.optString("mime_type", getMimeType(origName))
                val id = pvf.nameWithoutExtension

                FileItem(
                    id = id,
                    name = origName,
                    path = pvf.absolutePath,
                    size = origSize,
                    mimeType = origMime,
                    lastModified = origModified,
                    isDirectory = false,
                    isVaultItem = true,
                    category = FileCategory.fromExtension(origName.substringAfterLast('.', "")),
                    originalVaultName = origName
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clearDecryptedCache() {
        runCatching {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }

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
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    companion object {
        private const val KEY_ALIAS = "PrismVaultMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private val MAGIC = byteArrayOf('P'.code.toByte(), 'V'.code.toByte(), 'F'.code.toByte(), '1'.code.toByte())

        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_PANIC_MODE = "panic_mode"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTO_LOCK = "auto_lock"
        const val MAX_FAILED_ATTEMPTS = 5
    }
}
