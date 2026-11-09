package com.example.ui.navigation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.ui.components.SkeuomorphicFolderIcon
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.BrowserViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HomeViewModel
import com.example.ui.screens.PreviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SettingsViewModel
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.VaultViewModel
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismMagenta
import java.io.File

enum class Screen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    BROWSER("Browse", Icons.Filled.Folder, Icons.Outlined.Folder),
    VAULT("Vault", Icons.Filled.Lock, Icons.Outlined.Lock),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun PrismFilesApp(
    homeViewModel: HomeViewModel = viewModel(),
    browserViewModel: BrowserViewModel = viewModel(),
    vaultViewModel: VaultViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var activePreviewItem by remember { mutableStateOf<FileItem?>(null) }
    var activePreviewFile by remember { mutableStateOf<File?>(null) }

    // Check storage permissions
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
        )
    }

    // Permission launcher for Android 11+ MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasStoragePermission = Environment.isExternalStorageManager()
            if (hasStoragePermission) {
                homeViewModel.loadDashboardData()
                browserViewModel.refreshFiles()
            }
        }
    }

    // Permission launcher for standard read/write permissions
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            hasStoragePermission = true
            homeViewModel.loadDashboardData()
            browserViewModel.refreshFiles()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            bottomBar = {
                if (!isExpandedScreen && activePreviewItem == null) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar")
                    ) {
                        Screen.values().forEach { screen ->
                            val isSelected = currentScreen == screen
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00363D),
                                    indicatorColor = if (screen == Screen.VAULT) PrismMagenta.copy(alpha = 0.35f) else PrismCyan,
                                    selectedTextColor = if (screen == Screen.VAULT) PrismMagenta else PrismCyan
                                ),
                                modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Navigation Rail for tablets / large screens
                if (isExpandedScreen && activePreviewItem == null) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        SkeuomorphicFolderIcon(size = 40.dp)
                        Spacer(modifier = Modifier.height(24.dp))

                        Screen.values().forEach { screen ->
                            val isSelected = currentScreen == screen
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00363D),
                                    indicatorColor = PrismCyan
                                )
                            )
                        }
                    }
                }

                // Main Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    // Permission Banner if storage access not yet granted
                    if (!hasStoragePermission) {
                        PermissionRationaleCard(
                            onGrantClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    try {
                                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        manageStorageLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                        manageStorageLauncher.launch(intent)
                                    }
                                } else {
                                    legacyPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_EXTERNAL_STORAGE,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        )
                                    )
                                }
                            }
                        )
                    }

                    // Screen Crossfade Transition
                    Crossfade(
                        targetState = currentScreen,
                        label = "screen_transition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            Screen.HOME -> HomeScreen(
                                viewModel = homeViewModel,
                                onNavigateToCategory = { category ->
                                    browserViewModel.setCategoryFilter(category)
                                    currentScreen = Screen.BROWSER
                                },
                                onNavigateToBrowser = { path ->
                                    if (path != null) browserViewModel.navigateToPath(path)
                                    currentScreen = Screen.BROWSER
                                },
                                onNavigateToVault = { currentScreen = Screen.VAULT },
                                onOpenFile = { item ->
                                    activePreviewItem = item
                                    activePreviewFile = null
                                },
                                onSearchClick = { currentScreen = Screen.BROWSER }
                            )
                            Screen.BROWSER -> BrowserScreen(
                                viewModel = browserViewModel,
                                onOpenFile = { item ->
                                    activePreviewItem = item
                                    activePreviewFile = null
                                }
                            )
                            Screen.VAULT -> VaultScreen(
                                viewModel = vaultViewModel,
                                onPreviewFile = { item, decryptedFile ->
                                    activePreviewItem = item
                                    activePreviewFile = decryptedFile
                                }
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateToVault = { currentScreen = Screen.VAULT }
                            )
                        }
                    }

                    // File Preview Modal Overlay
                    val previewItem = activePreviewItem
                    if (previewItem != null) {
                        PreviewScreen(
                            fileItem = previewItem,
                            previewFile = activePreviewFile,
                            onBack = {
                                activePreviewItem = null
                                activePreviewFile = null
                            },
                            onHideToVault = {
                                browserViewModel.toggleSelection(previewItem)
                                browserViewModel.hideSelectedToVault {
                                    activePreviewItem = null
                                    activePreviewFile = null
                                }
                            },
                            onDelete = {
                                if (previewItem.isVaultItem) {
                                    vaultViewModel.deleteVaultFile(previewItem)
                                } else {
                                    browserViewModel.deleteItem(previewItem)
                                }
                                activePreviewItem = null
                                activePreviewFile = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRationaleCard(onGrantClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SkeuomorphicFolderIcon(size = 56.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Storage Access Required",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Prism Files needs access to manage and secure your files on device. All processing and encryption stay 100% private and on-device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrismCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("grant_permission_button")
                ) {
                    Text(
                        text = "Grant Storage Access",
                        color = Color(0xFF00363D),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
