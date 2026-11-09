package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.StorageStats
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassDialog
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.SkeuomorphicFileIcon
import com.example.ui.components.SkeuomorphicFolderIcon
import com.example.ui.components.StorageRingChart
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismMagenta
import com.example.ui.theme.PrismMint
import com.example.ui.theme.PrismPurple

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCategory: (FileCategory) -> Unit,
    onNavigateToBrowser: (String?) -> Unit,
    onNavigateToVault: () -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCleanDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 1. Centered M3 Expressive Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Prism",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    brush = Brush.horizontalGradient(
                                        listOf(PrismCyan, PrismPurple, PrismMagenta)
                                    )
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Files",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        Text(
                            text = "Tactile. Private. Secure.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("home_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.loadDashboardData() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("home_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 2. Storage Ring Chart & Metrics
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Device Storage",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.storageStats.formattedFree} free",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = PrismMint,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        StorageRingChart(
                            stats = uiState.storageStats,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Category mini legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StorageLegendItem(
                                color = FileCategory.IMAGES.primaryColor,
                                label = "Images",
                                size = StorageStats.formatBytes(uiState.storageStats.imagesBytes)
                            )
                            StorageLegendItem(
                                color = FileCategory.VIDEOS.primaryColor,
                                label = "Videos",
                                size = StorageStats.formatBytes(uiState.storageStats.videosBytes)
                            )
                            StorageLegendItem(
                                color = FileCategory.DOCUMENTS.primaryColor,
                                label = "Docs",
                                size = StorageStats.formatBytes(uiState.storageStats.documentsBytes)
                            )
                            StorageLegendItem(
                                color = FileCategory.VAULT.primaryColor,
                                label = "Vault",
                                size = StorageStats.formatBytes(uiState.storageStats.vaultBytes)
                            )
                        }
                    }
                }
            }

            // 3. Neumorphic "Smart Clean" Card
            item {
                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(PrismCyan.copy(alpha = 0.25f), PrismPurple.copy(alpha = 0.25f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Smart Clean",
                                tint = PrismCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart Clean",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.smartCleanResult.formattedReclaimable} reclaimable space",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showCleanDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrismCyan,
                                contentColor = Color(0xFF00363D)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("smart_clean_button")
                        ) {
                            if (uiState.isCleaning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF00363D),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Clean",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 4. Categories Section (Skeuomorphic file and folder icons)
            item {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            item {
                val categories = listOf(
                    FileCategory.IMAGES,
                    FileCategory.VIDEOS,
                    FileCategory.AUDIO,
                    FileCategory.DOCUMENTS,
                    FileCategory.DOWNLOADS,
                    FileCategory.ARCHIVES,
                    FileCategory.APPS,
                    FileCategory.VAULT
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .padding(horizontal = 12.dp),
                    userScrollEnabled = false
                ) {
                    items(categories) { category ->
                        CategoryGridTile(
                            category = category,
                            onClick = {
                                if (category == FileCategory.VAULT) {
                                    onNavigateToVault()
                                } else {
                                    onNavigateToCategory(category)
                                }
                            }
                        )
                    }
                }
            }

            // 5. Recent Files Carousel
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Files",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { onNavigateToBrowser(null) }) {
                        Text(
                            text = "Browse all",
                            color = PrismCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                if (uiState.recentFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SkeuomorphicFolderIcon(size = 48.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No recent files found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.recentFiles) { fileItem ->
                            RecentFileCarouselCard(
                                item = fileItem,
                                onClick = { onOpenFile(fileItem) }
                            )
                        }
                    }
                }
            }
        }

        // Clean Dialog
        GlassDialog(
            visible = showCleanDialog,
            onDismissRequest = { showCleanDialog = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = PrismCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Smart Clean Scan",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Prism Files analyzed storage and identified items that can be safely reclaimed:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                CleanBreakdownRow(
                    title = "App Cache & Logs",
                    count = uiState.smartCleanResult.cacheFiles.size,
                    size = StorageStats.formatBytes(uiState.smartCleanResult.cacheFiles.sumOf { it.size })
                )
                CleanBreakdownRow(
                    title = "Temporary Files",
                    count = uiState.smartCleanResult.temporaryFiles.size,
                    size = StorageStats.formatBytes(uiState.smartCleanResult.temporaryFiles.sumOf { it.size })
                )
                CleanBreakdownRow(
                    title = "Duplicate Candidates",
                    count = uiState.smartCleanResult.duplicateFiles.size,
                    size = StorageStats.formatBytes(uiState.smartCleanResult.duplicateFiles.sumOf { it.size })
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showCleanDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.runSmartClean()
                            showCleanDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrismCyan)
                    ) {
                        Text(
                            text = "Clean Now (${uiState.smartCleanResult.formattedReclaimable})",
                            color = Color(0xFF00363D),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Cleaned Message Banner
        AnimatedVisibility(
            visible = uiState.cleanedBytesMessage != null,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp, start = 16.dp, end = 16.dp)
        ) {
            uiState.cleanedBytesMessage?.let { msg ->
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.dismissCleanMessage() }) {
                            Text("OK", color = PrismCyan)
                        }
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PrismMint
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryGridTile(
    category: FileCategory,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (category == FileCategory.VAULT) {
            SkeuomorphicFileIcon(
                category = FileCategory.VAULT,
                extension = "pvf",
                size = 46.dp
            )
        } else {
            SkeuomorphicFolderIcon(
                size = 46.dp,
                accentColor = category.primaryColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = category.title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecentFileCarouselCard(
    item: FileItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("recent_file_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.category.primaryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                SkeuomorphicFileIcon(
                    category = item.category,
                    extension = item.extension,
                    size = 38.dp
                )
            }

            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.formattedSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StorageLegendItem(
    color: Color,
    label: String,
    size: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = size,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CleanBreakdownRow(
    title: String,
    count: Int,
    size: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$title ($count items)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = size,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = PrismCyan
        )
    }
}
