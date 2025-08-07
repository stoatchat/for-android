package chat.revolt.api.routes.googlesheets

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable

/**
 * Repository for fetching server data from a Google Sheet
 */
class ServerDataRepository {

    /**
     * Fetches server data from Google Sheets and returns as a Flow
     * @param sheetUrl The URL of the published Google Sheet (CSV or JSON)
     * @return Flow of ServerData list
     */
    fun getServers(sheetUrl: String): Flow<List<ServerData>> = flow {
        val servers = GoogleSheetsService.fetchSheetData(
            sheetUrl = sheetUrl
        ) { rowData ->
            ServerData(
                id = rowData["id"] ?: "",
                name = rowData["name"] ?: "",
                description = rowData["description"] ?: "",
                inviteCode = rowData["inviteCode"] ?: "",
                disabled = when (rowData["disabled"]?.lowercase()) {
                    "false" -> false
                    else -> true
                },
                showColor = rowData["showcolor"]
            )
        }
        emit(servers)
    }.flowOn(Dispatchers.IO)

}

/**
 * Data class representing a server
 */
@Serializable
data class ServerData(
    val id: String,
    val name: String,
    val description: String,
    val inviteCode: String,
    val disabled: Boolean,
    val showColor: String?,
)