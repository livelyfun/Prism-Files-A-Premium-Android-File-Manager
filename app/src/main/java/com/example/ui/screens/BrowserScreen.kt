package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassDialog
import com.example.ui.components.SkeuomorphicFileIcon
import com.example.ui.components.SkeuomorphicFolderIcon
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismMint
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onOpenFile: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Handle back button for breadcrumbs
    BackHandler(enabled = uiState.breadcrumbs.size > 1) {
        val parentPath = uiState.breadcrumbs.getOrNull(uiState.breadcrumbs.size - 2)?.second
        if (parentPath != null) {
            viewModel.navigateToPath(parentPath)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Action Bar or Multi-select Bar
            if (uiState.isMultiSelectMode) {
                MultiSelectBar(
                    selectedCount = uiState.selectedItems.size,
                    onSelectAll = { viewModel.selectAll() },
                    onClearSelection = { viewModel.clearSelection() },
                    onDelete = { viewModel.deleteSelectedItems() },
                    onHideToVault = {
                        viewModel.hideSelectedToVault { count ->
                            // Handled via snackbar
                        }
                    },
                    onShare = {
                        val selectedPaths = uiState.selectedItems
                        val selectedFiles = uiState.files.filter { it.path in selectedPaths && !it.isDirectory }
                        if (selectedFiles.isNotEmpty()) {
                            val uris = selectedFiles.mapNotNull {
                                runCatching {
                                    FileProvider.getUriForFile(context, "${context.packageName}.provider", File(it.path))
                                }.getOrNull()
                            }
                            if (uris.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "*/*"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Files"))
                            }
                        }
                    }
                )
            } else {
                BrowserHeader(
                    searchActive = isSearchActive,
                    searchQuery = uiState.searchQuery,
                    onSearchToggle = { isSearchActive = !isSearchActive },
                    onSearchChange = { viewModel.onSearchQueryChange(it) },
                    isGridView = uiState.isGridView,
                    onToggleView = { viewModel.toggleViewMode() },
                    onNewFolder = {
                        newFolderName = ""
                        showNewFolderDialog = true
                    }
                )
            }

            // 2. Breadcrumb Navigation Bar
            BreadcrumbBar(
                breadcrumbs = uiState.breadcrumbs,
                onBreadcrumbClick = { path -> viewModel.navigateToPath(path) }
            )

            // 3. Sort & Filter Chips Row
            SortAndFilterRow(
                currentSort = uiState.sortType,
                isAscending = uiState.sortAscending,
                selectedCategory = uiState.selectedCategory,
                onSortSelect = { viewModel.setSort(it) },
                onCategorySelect = { viewModel.setCategoryFilter(it) }
            )

            // 4. File List / Grid
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrismCyan)
                }
            } else if (uiState.files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SkeuomorphicFolderIcon(size = 64.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "No files match your search" else "This folder is empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (uiState.isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 90.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.files, key = { it.path }) { item ->
                            val isSelected = item.path in uiState.selectedItems
                            GridFileCard(
                                item = item,
                                isSelected = isSelected,
                                isMultiSelect = uiState.isMultiSelectMode,
                                onClick = {
                                    if (uiState.isMultiSelectMode) {
                                        viewModel.toggleSelection(item)
                                    } else {
                                        if (item.isDirectory) {
                                            viewModel.navigateToPath(item.path)
                                        } else {
                                            onOpenFile(item)
                                        }
                                    }
                                },
                                onLongClick = { viewModel.toggleSelection(item) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
                    ) {
                        items(uiState.files, key = { it.path }) { item ->
                            val isSelected = item.path in uiState.selectedItems

                            // Swipe to dismiss: swipe left to delete, swipe right to share
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.deleteItem(item)
                                        true
                                    } else if (value == SwipeToDismissBoxValue.StartToEnd) {
                                        // Share file
                                        runCatching {
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", File(item.path))
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = item.mimeType
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share ${item.name}"))
                                        }
                                        false
                                    } else false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val direction = dismissState.dismissDirection
                                    val color = if (direction == SwipeToDismissBoxValue.EndToStart) Color(0xFFFF5252) else PrismCyan
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = if (direction == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
                                    ) {
                                        Icon(
                                            imageVector = if (direction == SwipeToDismissBoxValue.EndToStart) Icons.Default.Delete else Icons.Default.Share,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }
                            ) {
                                ListFileRow(
                                    item = item,
                                    isSelected = isSelected,
                                    isMultiSelect = uiState.isMultiSelectMode,
                                    onClick = {
                                        if (uiState.isMultiSelectMode) {
                                            viewModel.toggleSelection(item)
                                        } else {
                                            if (item.isDirectory) {
                                                viewModel.navigateToPath(item.path)
                                            } else {
                                                onOpenFile(item)
                                            }
                                        }
                                    },
                                    onLongClick = { viewModel.toggleSelection(item) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                newFolderName = ""
                showNewFolderDialog = true
            },
            containerColor = PrismCyan,
            contentColor = Color(0xFF00363D),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp)
                .testTag("create_folder_fab")
        ) {
            Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Create Folder")
        }

        // Create Folder Dialog
        GlassDialog(
            visible = showNewFolderDialog,
            onDismissRequest = { showNewFolderDialog = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "New Folder",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("Folder name") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("new_folder_input")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showNewFolderDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.createFolder(newFolderName.trim())
                            showNewFolderDialog = false
                        },
                        enabled = newFolderName.isNotBlank()
                    ) {
                        Text("Create", color = PrismCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Feedback Snackbar
        AnimatedVisibility(
            visible = uiState.snackbarMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 75.dp, start = 16.dp, end = 16.dp)
        ) {
            uiState.snackbarMessage?.let { msg ->
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.dismissSnackbar() }) {
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
private fun BrowserHeader(
    searchActive: Boolean,
    searchQuery: String,
    onSearchToggle: () -> Unit,
    onSearchChange: (String) -> Unit,
    isGridView: Boolean,
    onToggleView: () -> Unit,
    onNewFolder: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (searchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search files...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("browser_search_input"),
                trailingIcon = {
                    IconButton(onClick = onSearchToggle) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close search")
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(24.dp)
            )
        } else {
            Text(
                text = "File Browser",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row {
                IconButton(
                    onClick = onSearchToggle,
                    modifier = Modifier.testTag("browser_search_toggle")
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search files")
                }
                IconButton(
                    onClick = onToggleView,
                    modifier = Modifier.testTag("browser_view_toggle")
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle view"
                    )
                }
                IconButton(onClick = onNewFolder) {
                    Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "New folder")
                }
            }
        }
    }
}

@Composable
private fun MultiSelectBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    onHideToVault: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClearSelection) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear selection")
            }
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Row {
            IconButton(onClick = onSelectAll) {
                Icon(imageVector = Icons.Default.SelectAll, contentDescription = "Select all")
            }
            IconButton(onClick = onShare) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share selected")
            }
            IconButton(onClick = onHideToVault) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Hide in Vault")
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete selected")
            }
        }
    }
}

@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<Pair<String, String>>,
    onBreadcrumbClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(breadcrumbs) { crumb ->
            val isLast = crumb == breadcrumbs.lastOrNull()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBreadcrumbClick(crumb.second) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = crumb.first,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLast) PrismCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            if (!isLast) {
                Text(
                    text = " › ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun SortAndFilterRow(
    currentSort: String,
    isAscending: Boolean,
    selectedCategory: FileCategory?,
    onSortSelect: (String) -> Unit,
    onCategorySelect: (FileCategory?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort Chips
        listOf("NAME" to "Name", "DATE" to "Date", "SIZE" to "Size", "TYPE" to "Type").forEach { (key, label) ->
            item {
                val isSelected = currentSort == key
                FilterChip(
                    selected = isSelected,
                    onClick = { onSortSelect(key) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = label)
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrismCyan.copy(alpha = 0.2f),
                        selectedLabelColor = PrismCyan
                    )
                )
            }
        }

        // Category Filter Reset
        if (selectedCategory != null) {
            item {
                FilterChip(
                    selected = true,
                    onClick = { onCategorySelect(null) },
                    label = { Text("Filter: ${selectedCategory.title} ✕") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = selectedCategory.primaryColor.copy(alpha = 0.25f),
                        selectedLabelColor = selectedCategory.primaryColor
                    )
                )
            }
        }
    }
}

@Composable
private fun ListFileRow(
    item: FileItem,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) PrismCyan.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (item.isDirectory) {
                SkeuomorphicFolderIcon(size = 40.dp)
            } else if (item.category == FileCategory.IMAGES && File(item.path).exists()) {
                AsyncImage(
                    model = File(item.path),
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                SkeuomorphicFileIcon(
                    category = item.category,
                    extension = item.extension,
                    size = 38.dp
                )
            }
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
                    text = item.formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isMultiSelect) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PrismCyan else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) PrismCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF00363D),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GridFileCard(
    item: FileItem,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrismCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (item.isDirectory) {
                    SkeuomorphicFolderIcon(size = 54.dp)
                } else if (item.category == FileCategory.IMAGES && File(item.path).exists()) {
                    AsyncImage(
                        model = File(item.path),
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    SkeuomorphicFileIcon(
                        category = item.category,
                        extension = item.extension,
                        size = 50.dp
                    )
                }

                if (isMultiSelect && isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(PrismCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF00363D),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.formattedSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
