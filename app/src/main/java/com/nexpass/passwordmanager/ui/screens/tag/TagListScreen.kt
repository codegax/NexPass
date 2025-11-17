package com.nexpass.passwordmanager.ui.screens.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nexpass.passwordmanager.domain.model.Tag
import com.nexpass.passwordmanager.ui.theme.NexPassTheme
import com.nexpass.passwordmanager.ui.viewmodel.TagViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagListScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf<Tag?>(null) }
    var tagName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(viewModel.availableColors.first()) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tags") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    tagName = ""
                    selectedColor = viewModel.availableColors.first()
                    showCreateDialog = true
                }
            ) {
                Icon(Icons.Default.Add, "Add Tag")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.tags.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No tags yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap + to create your first tag",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.tags, key = { it.id }) { tag ->
                    TagListItem(
                        tag = tag,
                        onEdit = {
                            selectedTag = tag
                            tagName = tag.name
                            selectedColor = tag.color
                            showEditDialog = true
                        },
                        onDelete = {
                            selectedTag = tag
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        // Create tag dialog
        if (showCreateDialog) {
            TagDialog(
                title = "Create Tag",
                name = tagName,
                onNameChange = { tagName = it },
                selectedColor = selectedColor,
                onColorSelect = { selectedColor = it },
                availableColors = viewModel.availableColors,
                onConfirm = {
                    viewModel.createTag(tagName, selectedColor)
                    showCreateDialog = false
                },
                onDismiss = {
                    showCreateDialog = false
                    tagName = ""
                }
            )
        }

        // Edit tag dialog
        if (showEditDialog && selectedTag != null) {
            TagDialog(
                title = "Edit Tag",
                name = tagName,
                onNameChange = { tagName = it },
                selectedColor = selectedColor,
                onColorSelect = { selectedColor = it },
                availableColors = viewModel.availableColors,
                onConfirm = {
                    selectedTag?.let {
                        viewModel.updateTag(it.id, tagName, selectedColor)
                    }
                    showEditDialog = false
                    selectedTag = null
                },
                onDismiss = {
                    showEditDialog = false
                    selectedTag = null
                    tagName = ""
                }
            )
        }

        // Delete tag dialog
        if (showDeleteDialog && selectedTag != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    selectedTag = null
                },
                title = { Text("Delete Tag?") },
                text = {
                    Text("Are you sure you want to delete \"${selectedTag?.name}\"? Passwords with this tag will not be deleted.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedTag?.let {
                                viewModel.deleteTag(it.id)
                            }
                            showDeleteDialog = false
                            selectedTag = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            selectedTag = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun TagListItem(
    tag: Tag,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Color indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(tag.color)))
                )

                Column {
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Created ${formatDate(tag.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit tag",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete tag",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun TagDialog(
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    selectedColor: String,
    onColorSelect: (String) -> Unit,
    availableColors: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Tag Name") },
                    placeholder = { Text("e.g., Work, Important") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Select Color",
                    style = MaterialTheme.typography.labelMedium
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableColors) { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .then(
                                    if (color == selectedColor) {
                                        Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onColorSelect(color) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = name.isNotBlank()
            ) {
                Text(if (title.contains("Create")) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
}

@Preview(showBackground = true)
@Composable
fun TagListScreenPreview() {
    NexPassTheme {
        TagListScreen(
            onNavigateBack = {}
        )
    }
}
