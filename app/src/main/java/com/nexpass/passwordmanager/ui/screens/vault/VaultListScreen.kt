package com.nexpass.passwordmanager.ui.screens.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nexpass.passwordmanager.ui.components.*
import com.nexpass.passwordmanager.ui.theme.NexPassTheme
import com.nexpass.passwordmanager.ui.viewmodel.VaultListViewModel
import com.nexpass.passwordmanager.ui.viewmodel.VaultListUiState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    onPasswordClick: (String) -> Unit,
    onCreatePassword: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VaultListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val folders by viewModel.folders.collectAsState()
    var isSearching by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            if (isSearching) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.search(it) },
                            placeholder = { Text("Search passwords...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearching = false
                            viewModel.search("")
                        }) {
                            Icon(Icons.Default.Search, "Close Search")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("My Vault") },
                    actions = {
                        IconToggleButton(
                            checked = showFavoritesOnly,
                            onCheckedChange = { viewModel.toggleFavoritesFilter() }
                        ) {
                            Icon(
                                imageVector = if (showFavoritesOnly) {
                                    Icons.Default.Star
                                } else {
                                    Icons.Outlined.StarBorder
                                },
                                contentDescription = "Filter Favorites",
                                tint = if (showFavoritesOnly) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreatePassword) {
                Icon(Icons.Default.Add, "Add Password")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Folder filter chip row - ALWAYS visible when folders exist
            if (folders.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Filter menu button
                    FilterChip(
                        selected = selectedFolderId != null,
                        onClick = { showFolderMenu = true },
                        label = {
                            Text(
                                if (selectedFolderId != null) {
                                    folders.find { it.id == selectedFolderId }?.name ?: "Folder"
                                } else {
                                    "All Folders"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (selectedFolderId != null) Icons.Default.Folder else Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = if (selectedFolderId != null) {
                            {
                                IconButton(
                                    onClick = { viewModel.clearFolderFilter() },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear filter",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else null
                    )

                    // Folder filter dropdown menu
                    DropdownMenu(
                        expanded = showFolderMenu,
                        onDismissRequest = { showFolderMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Folders") },
                            onClick = {
                                viewModel.clearFolderFilter()
                                showFolderMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.FilterList, contentDescription = null)
                            }
                        )
                        HorizontalDivider()
                        folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                onClick = {
                                    viewModel.setFolderFilter(folder.id)
                                    showFolderMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Folder, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }

            // Content area
            when (val state = uiState) {
                is VaultListUiState.Loading -> {
                    LoadingState(
                        message = "Loading passwords...",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is VaultListUiState.Empty -> {
                    EmptyVaultState(
                        onCreatePassword = onCreatePassword,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is VaultListUiState.EmptySearch -> {
                    EmptySearchState(
                        query = state.query,
                        onClearSearch = {
                            isSearching = false
                            viewModel.search("")
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is VaultListUiState.EmptyFavorites -> {
                    EmptyState(
                        icon = Icons.Outlined.StarBorder,
                        title = "No Favorites",
                        message = "You haven't marked any passwords as favorites yet. Tap the star icon on a password to add it to favorites.",
                        actionLabel = "Show All",
                        onAction = { viewModel.toggleFavoritesFilter() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is VaultListUiState.Success -> {
                    // Password list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.passwords, key = { it.id }) { password ->
                            NexPassCard(
                                title = password.title,
                                subtitle = password.username,
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (password.favorite) {
                                            Icons.Default.Star
                                        } else {
                                            Icons.Outlined.StarBorder
                                        },
                                        contentDescription = "Favorite",
                                        tint = if (password.favorite) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                },
                                onClick = { onPasswordClick(password.id) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                is VaultListUiState.Error -> {
                    ErrorState(
                        error = state.error,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is VaultListUiState.Locked -> {
                    LockedVaultState(
                        onUnlock = { /* Navigate to unlock screen */ },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VaultListScreenPreview() {
    NexPassTheme {
        VaultListScreen(
            onPasswordClick = {},
            onCreatePassword = {},
            onNavigateToSettings = {}
        )
    }
}
