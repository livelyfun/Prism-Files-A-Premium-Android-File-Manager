package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassSurface
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.NeumorphicPinKey
import com.example.ui.components.NeumorphicSwitch
import com.example.ui.components.PrismaticMeshBackground
import com.example.ui.components.SkeuomorphicFileIcon
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismMagenta
import com.example.ui.theme.PrismMint
import com.example.ui.theme.PrismPurple
import java.io.File

@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onPreviewFile: (FileItem, File) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // SAF File Picker for importing external files into vault
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            runCatching {
                // Copy stream to temp file and import
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.tmp")
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.importFile(tempFile)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkInitialVaultState()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!uiState.isUnlocked) {
            // Glassmorphic Unlock & PIN Screen
            VaultLockScreen(
                uiState = uiState,
                onDigit = { viewModel.onDigit(it) },
                onBackspace = { viewModel.onBackspace() },
                onBiometric = {
                    if (activity != null) {
                        viewModel.triggerBiometricUnlock(activity)
                    }
                },
                onResetPanic = { viewModel.resetPanicMode() }
            )
        } else {
            // Unlocked Vault Dashboard
            VaultContentScreen(
                uiState = uiState,
                onLock = { viewModel.lockVault() },
                onAddFiles = { filePickerLauncher.launch(arrayOf("*/*")) },
                onAutoLockChange = { viewModel.setAutoLock(it) },
                onBiometricChange = { viewModel.setBiometric(it) },
                onUnhide = { item -> viewModel.unhideFile(item) },
                onDelete = { item -> viewModel.deleteVaultFile(item) },
                onPreview = { item ->
                    viewModel.decryptForPreview(item) { decryptedFile ->
                        onPreviewFile(item, decryptedFile)
                    }
                }
            )
        }

        // Status Feedback Snackbar
        AnimatedVisibility(
            visible = uiState.statusMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 75.dp, start = 16.dp, end = 16.dp)
        ) {
            uiState.statusMessage?.let { msg ->
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.dismissStatus() }) {
                            Text("OK", color = PrismCyan)
                        }
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Text(text = msg)
                }
            }
        }
    }
}

@Composable
private fun VaultLockScreen(
    uiState: VaultUiState,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: () -> Unit,
    onResetPanic: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        PrismaticMeshBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Glassmorphic Header Card
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (uiState.isPanicMode) Color(0xFFFF5252).copy(alpha = 0.3f)
                                        else PrismMagenta.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isPanicMode) Icons.Default.Warning else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (uiState.isPanicMode) Color(0xFFFF5252) else PrismMagenta,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (uiState.isSettingUpPin) {
                            if (uiState.isConfirmingPin) "Confirm Vault PIN" else "Set Vault PIN"
                        } else {
                            if (uiState.isPanicMode) "Panic Mode Lockdown" else "Prism Vault"
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (uiState.isSettingUpPin) {
                            "Create a 4-digit master PIN to secure your vault"
                        } else if (uiState.isPanicMode) {
                            "5 consecutive failed PIN attempts detected. Vault is concealed."
                        } else {
                            "Hardware-backed AES-256 GCM encrypted storage"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!uiState.isPanicMode) {
                        // PIN Dots Indicator
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            for (i in 0 until 4) {
                                val isFilled = i < uiState.pinInput.length
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isFilled) PrismCyan else Color.White.copy(alpha = 0.25f)
                                        )
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onResetPanic,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                        ) {
                            Text("Reset Panic Lockdown", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Neumorphic PIN Keypad
            if (!uiState.isPanicMode) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val keyRows = listOf(
                        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ")
                    )

                    for (row in keyRows) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for ((digit, label) in row) {
                                NeumorphicPinKey(
                                    digit = digit,
                                    subLabel = label,
                                    onClick = { onDigit(digit) }
                                )
                            }
                        }
                    }

                    // Bottom row: Biometric / 0 / Backspace
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Biometric trigger button
                        if (uiState.isBiometricEnabled && uiState.isBiometricAvailable && !uiState.isSettingUpPin) {
                            IconButton(
                                onClick = onBiometric,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(PrismPurple.copy(alpha = 0.2f))
                                    .testTag("vault_biometric_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Unlock with Biometrics",
                                    tint = PrismPurple,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(72.dp))
                        }

                        NeumorphicPinKey(
                            digit = "0",
                            onClick = { onDigit("0") }
                        )

                        IconButton(
                            onClick = onBackspace,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .testTag("vault_backspace_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun VaultContentScreen(
    uiState: VaultUiState,
    onLock: () -> Unit,
    onAddFiles: () -> Unit,
    onAutoLockChange: (Boolean) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onUnhide: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onPreview: (FileItem) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Hero Vault Header with Graphic Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.vault_hero_banner_1794229482833),
                        contentDescription = "Encrypted Vault Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xFF0C1017))
                                )
                            )
                    )

                    // Top action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = PrismCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vault Unlocked",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Button(
                            onClick = onLock,
                            colors = ButtonDefaults.buttonColors(containerColor = PrismMagenta),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("lock_vault_button")
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock Now", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Bottom info in hero
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = PrismMint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AES-256-GCM Hardware Encrypted",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrismMint
                                )
                            )
                        }
                        Text(
                            text = "${uiState.vaultFiles.size} hidden items • .nomedia shielded",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Neumorphic Security Controls
            item {
                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "Vault Security Controls",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NeumorphicSwitch(
                            checked = uiState.autoLockOnExit,
                            onCheckedChange = onAutoLockChange,
                            label = "Auto-Lock On Exit",
                            subtitle = "Clears decrypted cache and locks when leaving vault"
                        )
                        NeumorphicSwitch(
                            checked = uiState.isBiometricEnabled,
                            onCheckedChange = onBiometricChange,
                            label = "Biometric Quick Unlock",
                            subtitle = "Allow unlocking using fingerprint or face"
                        )
                    }
                }
            }

            // Vault Files Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Encrypted Files",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.vaultFiles.size} files",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Encrypted Files List
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrismCyan)
                    }
                }
            } else if (uiState.vaultFiles.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SkeuomorphicFileIcon(
                                category = FileCategory.VAULT,
                                extension = "pvf",
                                size = 60.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Your Vault is Empty",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select photos, videos, or documents from the browser or tap '+' to encrypt and hide them here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onAddFiles,
                                colors = ButtonDefaults.buttonColors(containerColor = PrismCyan)
                            ) {
                                Text("Import Files to Vault", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(uiState.vaultFiles, key = { it.id }) { vaultItem ->
                    VaultFileRow(
                        item = vaultItem,
                        onPreview = { onPreview(vaultItem) },
                        onUnhide = { onUnhide(vaultItem) },
                        onDelete = { onDelete(vaultItem) }
                    )
                }
            }
        }

        // FAB to import new files into vault
        FloatingActionButton(
            onClick = onAddFiles,
            containerColor = PrismMagenta,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp)
                .testTag("vault_add_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add to Vault")
        }
    }
}

@Composable
private fun VaultFileRow(
    item: FileItem,
    onPreview: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPreview() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrismMagenta.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                SkeuomorphicFileIcon(
                    category = item.category,
                    extension = item.extension,
                    size = 38.dp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text(
                        text = item.formattedSize,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "AES-256 Encrypted",
                        style = MaterialTheme.typography.labelSmall.copy(color = PrismCyan)
                    )
                }
            }

            // Action Buttons
            Row {
                IconButton(onClick = onPreview) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Preview",
                        tint = PrismCyan
                    )
                }
                IconButton(onClick = onUnhide) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export & Unhide",
                        tint = PrismMint
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252)
                    )
                }
            }
        }
    }
}
