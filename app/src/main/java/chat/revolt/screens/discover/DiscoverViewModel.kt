package chat.revolt.screens.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.revolt.api.routes.googlesheets.ServerCategory
import chat.revolt.api.routes.googlesheets.ServerData
import chat.revolt.api.routes.googlesheets.ServerDataRepository
import chat.revolt.api.routes.server.ackServer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Discover screen
 * Handles fetching and managing server data from Google Sheets
 */
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val serverDataRepository: ServerDataRepository
) : ViewModel() {

    // UI state
    var uiState by mutableStateOf<DiscoverUiState>(DiscoverUiState.Loading)
        private set

    // Selected category for filtering
    var selectedCategory by mutableStateOf<String?>(null)
        private set

    // Search query for filtering
    var searchQuery by mutableStateOf("")
        private set

    // Initialize the ViewModel
    init {
        loadServerData()
    }

    /**
     * Loads server data from the repository
     */
    fun loadServerData() {
        uiState = DiscoverUiState.Loading
        viewModelScope.launch {
            serverDataRepository.getServers(
                sheetUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vRY41D-NgTE6bC3kTN3dRpisI-DoeHG8Eg7n31xb1CdydWjOLaphqYckkTiaG9oIQSWP92h3NE-7cpF/pub?gid=0&single=true&output=csv"
            )
                .catch { exception ->
                    uiState = DiscoverUiState.Error(exception.message ?: "Unknown error")
                }
                .collect { servers ->
                    serverDataRepository.getServerCategories(
                        sheetUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vRY41D-NgTE6bC3kTN3dRpisI-DoeHG8Eg7n31xb1CdydWjOLaphqYckkTiaG9oIQSWP92h3NE-7cpF/pub?gid=0&single=true&output=csv"
                    )
                        .catch { exception ->
                            uiState = DiscoverUiState.Error(exception.message ?: "Unknown error")
                        }
                        .collect { categories ->
                            uiState = DiscoverUiState.Success(
                                servers = servers,
                                categories = categories
                            )
                        }
                }
        }
    }

    /**
     * Sets the selected category for filtering
     * @param categoryId The ID of the category to select, or null to clear selection
     */
    fun selectCategory(categoryId: String?) {
        selectedCategory = categoryId
    }

    /**
     * Updates the search query for filtering
     * @param query The search query
     */
    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    /**
     * Returns the filtered list of servers based on selected category and search query
     */
    fun getFilteredServers(): List<ServerData> {
        val currentState = uiState
        if (currentState !is DiscoverUiState.Success) return emptyList()

        return currentState.servers.filter { server ->
            val matchesCategory = selectedCategory == null || server.category == selectedCategory
            val matchesSearch = searchQuery.isEmpty() || 
                server.name.contains(searchQuery, ignoreCase = true) || 
                server.description.contains(searchQuery, ignoreCase = true) ||
                server.tags.any { it.contains(searchQuery, ignoreCase = true) }
            
            matchesCategory && matchesSearch
        }
    }

    /**
     * Acknowledges a server selection
     * This can be used for analytics or to mark a server as viewed
     * @param serverId The ID of the selected server
     */
    fun acknowledgeServerSelection(serverId: String) {
        viewModelScope.launch {
            try {
                ackServer(serverId)
            } catch (e: Exception) {
                // Log but don't change UI state as this is not critical
                e.printStackTrace()
            }
        }
    }
}

/**
 * Sealed class representing the UI state of the Discover screen
 */
sealed class DiscoverUiState {
    /**
     * Loading state
     */
    object Loading : DiscoverUiState()
    
    /**
     * Success state with data
     * @param servers The list of servers
     * @param categories The list of server categories
     */
    data class Success(
        val servers: List<ServerData>,
        val categories: List<ServerCategory>
    ) : DiscoverUiState()
    
    /**
     * Error state
     * @param message The error message
     */
    data class Error(val message: String) : DiscoverUiState()
} 