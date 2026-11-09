package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesManager
import com.example.security.VaultManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: String = "SYSTEM", // SYSTEM, DARK, LIGHT
    val isBiometricEnabled: Boolean = true,
    val autoLockTimeout: String = "IMMEDIATE", // IMMEDIATE, 1_MIN, 5_MIN
    val isPanicMode: Boolean = false,
    val isPinSet: Boolean = false,
    val cacheCleanedMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val vaultManager = VaultManager(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            preferencesManager.themeModeFlow.collect { mode ->
                _uiState.value = _uiState.value.copy(
                    themeMode = mode,
                    isBiometricEnabled = vaultManager.isBiometricEnabled,
                    isPanicMode = vaultManager.isPanicModeActive,
                    isPinSet = vaultManager.isPinSet
                )
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
            _uiState.value = _uiState.value.copy(themeMode = mode)
        }
    }

    fun setBiometric(enabled: Boolean) {
        vaultManager.isBiometricEnabled = enabled
        _uiState.value = _uiState.value.copy(isBiometricEnabled = enabled)
    }

    fun setAutoLockTimeout(timeout: String) {
        viewModelScope.launch {
            preferencesManager.setAutoLockTimeout(timeout)
            _uiState.value = _uiState.value.copy(autoLockTimeout = timeout)
        }
    }

    fun resetPanicMode() {
        vaultManager.resetPanicMode()
        _uiState.value = _uiState.value.copy(isPanicMode = false)
    }

    fun clearTempCache() {
        vaultManager.clearDecryptedCache()
        _uiState.value = _uiState.value.copy(cacheCleanedMessage = "Decrypted temp cache wiped clean")
    }

    fun dismissCacheMessage() {
        _uiState.value = _uiState.value.copy(cacheCleanedMessage = null)
    }
}
