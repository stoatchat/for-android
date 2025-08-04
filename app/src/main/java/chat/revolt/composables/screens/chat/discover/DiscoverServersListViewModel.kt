package chat.revolt.composables.screens.chat.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.revolt.api.routes.googlesheets.ServerData
import chat.revolt.api.routes.googlesheets.ServerDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverServersListViewModel @Inject constructor(
    private val serverDataRepository: ServerDataRepository,
) : ViewModel() {
    
    // Use StateFlow to properly expose the server list to the UI
    private val _servers = MutableStateFlow<List<ServerData>>(emptyList())
    val servers: StateFlow<List<ServerData>> = _servers.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Initialize by loading servers
    init {
        loadServers()
    }
    
    fun loadServers() {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // Use the Google Sheets published CSV URL since that's what you were using before
                val csvUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vRY41D-NgTE6bC3kTN3dRpisI-DoeHG8Eg7n31xb1CdydWjOLaphqYckkTiaG9oIQSWP92h3NE-7cpF/pub?gid=0&single=true&output=csv"
                
                serverDataRepository.getServers(csvUrl)
                    .collect { serverList ->
                        _servers.value = serverList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
                _isLoading.value = false
            }
        }
    }
} 