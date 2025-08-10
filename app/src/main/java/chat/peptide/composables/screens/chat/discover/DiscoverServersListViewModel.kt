package chat.peptide.composables.screens.chat.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.peptide.api.RevoltError
import chat.peptide.api.routes.googlesheets.ServerData
import chat.peptide.api.routes.googlesheets.ServerDataRepository
import chat.peptide.api.routes.invites.fetchInviteByCode
import chat.peptide.api.routes.invites.joinInviteByCode
import chat.peptide.api.schemas.Invite
import chat.peptide.api.schemas.InviteJoined
import chat.peptide.api.schemas.RsResult
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

/**
 * Combined UI state for the discover servers screen
 */
data class DiscoverUiState(
    // Server list state
    val servers: List<ServerData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Server invite state
    val selectedInviteCode: String? = null,
    val processingServerId: String? = null,
    val loadedInviteData: Invite? = null,
    val loadedError: RevoltError? = null
)

@HiltViewModel
class DiscoverServersListViewModel @Inject constructor(
    private val serverDataRepository: ServerDataRepository,
) : ViewModel() {
    
    // Single UI state
    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()
    
    // Initialize
    init {
        loadServers()
    }
    
    /**
     * Load server list from repository
     */
    fun loadServers() {
        updateState { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                val csvUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vRY41D-NgTE6bC3kTN3dRpisI-DoeHG8Eg7n31xb1CdydWjOLaphqYckkTiaG9oIQSWP92h3NE-7cpF/pub?gid=0&single=true&output=csv"
                serverDataRepository.getServers(csvUrl).collect { servers ->
                    updateState { it.copy(servers = servers, isLoading = false) }
                }
            } catch (e: Exception) {
                updateState { 
                    it.copy(
                        error = e.message ?: "Unknown error occurred",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Load server data and show dialog when ready
     */
    fun loadServerDataAndShowDialog(inviteCode: String, serverId: String) {
        updateState { 
            it.copy(
                loadedInviteData = null,
                processingServerId = serverId
            )
        }
        
        viewModelScope.launch {
            try {
                val result = fetchInviteByCode(inviteCode)
                if (result.ok) {
                    updateState { 
                        it.copy(
                            loadedInviteData = result.value,
                            selectedInviteCode = inviteCode,
                            processingServerId = null
                        )
                    }
                } else {
                    updateState { 
                        it.copy(
                            loadedError = result.error,
                            selectedInviteCode = inviteCode,
                            processingServerId = null
                        )
                    }
                }
            } catch (_: Exception) {
                updateState { 
                    it.copy(
                        loadedError = RevoltError("Unknown"),
                        selectedInviteCode = inviteCode,
                        processingServerId = null
                    )
                }
            }
        }
    }
    
    /**
     * Join server without showing loading in the server list
     */
    fun joinServerWithoutProcessingIndicator(inviteCode: String): Flow<RsResult<InviteJoined, RevoltError>> = flow {
        try {
            emit(joinInviteByCode(inviteCode))
        } catch (_: Exception) {
            emit(RsResult.err(RevoltError("Unknown")))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Clear loaded data when dialog is dismissed
     */
    fun clearLoadedData() {
        updateState { 
            it.copy(
                loadedInviteData = null,
                loadedError = null,
                selectedInviteCode = null
            )
        }
    }
    
    /**
     * Set selected invite code
     */
    fun setSelectedInviteCode(code: String?) {
        updateState { it.copy(selectedInviteCode = code) }
    }
    
    /**
     * Helper function to update state
     */
    private fun updateState(update: (DiscoverUiState) -> DiscoverUiState) {
        _uiState.value = update(_uiState.value)
    }
}