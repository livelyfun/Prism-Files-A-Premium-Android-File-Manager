package com.example.ui.screens

import android.app.Application
import android.os.Environment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.FileItem
import com.example.security.BiometricAuthManager
import com.example.security.VaultManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class VaultUiState(
    val isUnlocked: Boolean = false,
    val isPinSet: Boolean = false,
    val isPanicMode: Boolean = false,
    val pinInput: String = "",
    val pinConfirmInput: String = "",
    val isSettingUpPin: Boolean = false,
    val isConfirmingPin: Boolean = false,
    val failedAttempts: Int = 0,
    val vaultFiles: List<FileItem> = emptyList(),
    val decryptedThumbnails: Map<String, File> = emptyMap(),
    val autoLockOnExit: Boolean = true,
    val isBiometricEnabled: Boolean = true,
    val isBiometricAvailable: Boolean = false,
    val statusMessage: String? = null,
    val isLoading: Boolean = false
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val vaultManager = VaultManager(application)
    private val biometricAuthManager = BiometricAuthManager(application)

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        checkInitialVaultState()
    }

    fun checkInitialVaultState() {
        val pinSet = vaultManager.isPinSet
        val panic = vaultManager.isPanicModeActive
        val attempts = vaultManager.failedAttempts
        val bioEnabled = vaultManager.isBiometricEnabled
        val bioAvailable = biometricAuthManager.isBiometricAvailable()
        val autoLock = vaultManager.autoLockOnExit

        _uiState.value = _uiState.value.copy(
            isPinSet = pinSet,
            isSettingUpPin = !pinSet,
            isPanicMode = panic,
            failedAttempts = attempts,
            isBiometricEnabled = bioEnabled,
            isBiometricAvailable = bioAvailable,
            autoLockOnExit = autoLock
        )
    }

    fun onDigit(digit: String) {
        val current = _uiState.value.pinInput
        if (current.length >= 6) return

        val next = current + digit
        _uiState.value = _uiState.value.copy(pinInput = next)

        if (_uiState.value.isSettingUpPin) {
            if (!_uiState.value.isConfirmingPin && next.length == 4) {
                // Move to confirmation step
                _uiState.value = _uiState.value.copy(
                    isConfirmingPin = true,
                    pinConfirmInput = next,
                    pinInput = "",
                    statusMessage = "Confirm your 4-digit PIN"
                )
            } else if (_uiState.value.isConfirmingPin && next.length == 4) {
                // Confirm matches
                if (next == _uiState.value.pinConfirmInput) {
                    vaultManager.setupPin(next)
                    _uiState.value = _uiState.value.copy(
                        isPinSet = true,
                        isSettingUpPin = false,
                        isConfirmingPin = false,
                        isUnlocked = true,
                        pinInput = "",
                        statusMessage = "Vault initialized and secured"
                    )
                    loadVaultFiles()
                } else {
                    _uiState.value = _uiState.value.copy(
                        pinInput = "",
                        statusMessage = "PINs do not match. Try again."
                    )
                }
            }
        } else {
            // Verify existing PIN
            if (next.length == 4) {
                verifyPin(next)
            }
        }
    }

    fun onBackspace() {
        val current = _uiState.value.pinInput
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(pinInput = current.dropLast(1))
        }
    }

    private fun verifyPin(pin: String) {
        if (vaultManager.verifyPin(pin)) {
            _uiState.value = _uiState.value.copy(
                isUnlocked = true,
                pinInput = "",
                failedAttempts = 0,
                statusMessage = null
            )
            loadVaultFiles()
        } else {
            val failed = vaultManager.failedAttempts
            val panic = vaultManager.isPanicModeActive
            _uiState.value = _uiState.value.copy(
                pinInput = "",
                failedAttempts = failed,
                isPanicMode = panic,
                statusMessage = if (panic) "Panic Mode Activated: Vault is locked" else "Incorrect PIN ($failed/5)"
            )
        }
    }

    fun triggerBiometricUnlock(activity: FragmentActivity) {
        if (!_uiState.value.isBiometricEnabled || !_uiState.value.isBiometricAvailable) return
        if (_uiState.value.isPanicMode) return

        biometricAuthManager.promptBiometric(
            activity = activity,
            title = "Unlock Prism Vault",
            subtitle = "Verify your fingerprint or face to decrypt vault",
            negativeButtonText = "Use PIN",
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    isUnlocked = true,
                    failedAttempts = 0,
                    statusMessage = null
                )
                loadVaultFiles()
            },
            onError = { err ->
                if (err != "Use PIN") {
                    _uiState.value = _uiState.value.copy(statusMessage = err)
                }
            }
        )
    }

    fun lockVault() {
        _uiState.value = _uiState.value.copy(
            isUnlocked = false,
            pinInput = "",
            statusMessage = null
        )
        vaultManager.clearDecryptedCache()
    }

    fun setAutoLock(enabled: Boolean) {
        vaultManager.autoLockOnExit = enabled
        _uiState.value = _uiState.value.copy(autoLockOnExit = enabled)
    }

    fun setBiometric(enabled: Boolean) {
        vaultManager.isBiometricEnabled = enabled
        _uiState.value = _uiState.value.copy(isBiometricEnabled = enabled)
    }

    fun resetPanicMode() {
        vaultManager.resetPanicMode()
        _uiState.value = _uiState.value.copy(
            isPanicMode = false,
            failedAttempts = 0,
            statusMessage = "Panic mode reset"
        )
    }

    fun loadVaultFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val files = vaultManager.getVaultFiles()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                vaultFiles = files
            )
        }
    }

    fun importFile(file: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = vaultManager.encryptAndImportFile(file)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(statusMessage = "Encrypted ${file.name} to Vault")
                loadVaultFiles()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Encryption failed: ${res.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun unhideFile(vaultItem: FileItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val res = vaultManager.exportAndUnhide(vaultItem, downloadsDir)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Restored ${vaultItem.name} to Downloads"
                )
                loadVaultFiles()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Failed to export: ${res.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun deleteVaultFile(vaultItem: FileItem) {
        viewModelScope.launch {
            vaultManager.deleteVaultItem(vaultItem)
            _uiState.value = _uiState.value.copy(statusMessage = "Permanently deleted ${vaultItem.name}")
            loadVaultFiles()
        }
    }

    fun decryptForPreview(vaultItem: FileItem, onReady: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = vaultManager.decryptToTempCache(vaultItem)
            _uiState.value = _uiState.value.copy(isLoading = false)
            res.onSuccess { tempFile ->
                onReady(tempFile)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(statusMessage = "Failed to decrypt: ${err.message}")
            }
        }
    }

    fun dismissStatus() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        if (vaultManager.autoLockOnExit) {
            vaultManager.clearDecryptedCache()
        }
    }
}
