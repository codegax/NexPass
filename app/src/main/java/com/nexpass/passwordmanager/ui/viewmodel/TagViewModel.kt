package com.nexpass.passwordmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpass.passwordmanager.domain.model.Tag
import com.nexpass.passwordmanager.domain.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for tag management.
 */
class TagViewModel(
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagUiState())
    val uiState: StateFlow<TagUiState> = _uiState.asStateFlow()

    // Predefined colors for tags
    val availableColors = listOf(
        "#F44336", // Red
        "#E91E63", // Pink
        "#9C27B0", // Purple
        "#673AB7", // Deep Purple
        "#3F51B5", // Indigo
        "#2196F3", // Blue
        "#03A9F4", // Light Blue
        "#00BCD4", // Cyan
        "#009688", // Teal
        "#4CAF50", // Green
        "#8BC34A", // Light Green
        "#CDDC39", // Lime
        "#FFEB3B", // Yellow
        "#FFC107", // Amber
        "#FF9800", // Orange
        "#FF5722"  // Deep Orange
    )

    init {
        loadTags()
    }

    /**
     * Load all tags.
     */
    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getAllFlow().collect { tags ->
                _uiState.value = _uiState.value.copy(
                    tags = tags,
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    /**
     * Create a new tag.
     */
    fun createTag(name: String, color: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Tag name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val tag = Tag(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    color = color,
                    createdAt = now,
                    updatedAt = now,
                    lastModified = now
                )
                tagRepository.insert(tag)
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to create tag: ${e.message}")
            }
        }
    }

    /**
     * Update an existing tag.
     */
    fun updateTag(tagId: String, newName: String, newColor: String) {
        if (newName.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Tag name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                val tag = tagRepository.getById(tagId)
                if (tag != null) {
                    val updatedTag = tag.copy(
                        name = newName.trim(),
                        color = newColor,
                        updatedAt = System.currentTimeMillis(),
                        lastModified = System.currentTimeMillis()
                    )
                    tagRepository.update(updatedTag)
                    _uiState.value = _uiState.value.copy(error = null)
                } else {
                    _uiState.value = _uiState.value.copy(error = "Tag not found")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to update tag: ${e.message}")
            }
        }
    }

    /**
     * Delete a tag.
     */
    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            try {
                tagRepository.delete(tagId)
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete tag: ${e.message}")
            }
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for tag management.
 */
data class TagUiState(
    val tags: List<Tag> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
