package chat.revolt.composables.screens.chat.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.revolt.api.RevoltError
import chat.revolt.api.routes.googlesheets.ServerData
import chat.revolt.api.routes.googlesheets.ServerDataRepository
import chat.revolt.api.routes.invites.fetchInviteByCode
import chat.revolt.api.routes.invites.joinInviteByCode
import chat.revolt.api.schemas.Invite
import chat.revolt.api.schemas.InviteJoined
import chat.revolt.api.schemas.RsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
    
    // Selected server invite code
    private val _selectedServerInviteCode = MutableStateFlow<String?>(null)
    val selectedServerInviteCodeFlow: StateFlow<String?> = _selectedServerInviteCode.asStateFlow()
    
    // Property for easy access to selectedServerInviteCode
    var selectedServerInviteCode: String?
        get() = _selectedServerInviteCode.value
        set(value) {
            _selectedServerInviteCode.value = value
        }
        
    // Currently processing server ID
    private val _processingServerId = MutableStateFlow<String?>(null)
    val processingServerId: StateFlow<String?> = _processingServerId.asStateFlow()
    
    // Server invite data that has been loaded
    private val _loadedInviteData = MutableStateFlow<Invite?>(null)
    val loadedInviteData: StateFlow<Invite?> = _loadedInviteData.asStateFlow()
    
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

    fun loadServerInviteInfo(inviteCode: String, serverId: String): Flow<RsResult<Invite, RevoltError>> = flow {
        _processingServerId.value = serverId
        _error.value = null
        
        try {
            val result = fetchInviteByCode(inviteCode)
            emit(result)
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
            emit(RsResult.err(RevoltError("Unknown")))
        } finally {
            _processingServerId.value = null
        }
    }.flowOn(Dispatchers.IO)
    
    fun joinServer(inviteCode: String): Flow<RsResult<InviteJoined, RevoltError>> = flow {
        _isLoading.value = true
        _error.value = null
        
        try {
            val result = joinInviteByCode(inviteCode)
            emit(result)
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
            emit(RsResult.err(RevoltError("Unknown")))
        } finally {
            _isLoading.value = false
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Load server data first, then show the dialog with the loaded data
     */
    fun loadServerDataAndShowDialog(inviteCode: String, serverId: String) {
        _loadedInviteData.value = null
        _processingServerId.value = serverId
        _error.value = null
        
        viewModelScope.launch {
            try {
                val result = fetchInviteByCode(inviteCode)
                if (result.ok) {
                    _loadedInviteData.value = result.value
                    // Only set the selected invite code after data is loaded
                    _selectedServerInviteCode.value = inviteCode
                } else {
                    _error.value = result.error?.type ?: "Unknown error occurred"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _processingServerId.value = null
            }
        }
    }
    
    /**
     * Clear loaded data when dialog is dismissed
     */
    fun clearLoadedData() {
        _loadedInviteData.value = null
        _selectedServerInviteCode.value = null
    }
} 