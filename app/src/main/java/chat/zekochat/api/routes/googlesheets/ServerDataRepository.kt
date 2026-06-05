package chat.zekochat.api.routes.googlesheets

import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable

/**
 * Repository for fetching server data. Tries the Public Servers API first,
 * falls back to Google Sheets CSV parsing on any failure.
 */
class ServerDataRepository {

    /**
     * Fetches server data from the public API first, falling back to Google Sheets.
     * @param sheetUrl The URL of the published Google Sheet (CSV or JSON) used as fallback
     * @return Flow of ServerData list
     */
    fun getServers(sheetUrl: String): Flow<List<ServerData>> = flow {
        // Attempt Public Servers API first
        try {
            val apiUrl = "https://manageapi.peptide.chat/api/directory/servers"
            val responseText = PeptideHttp.get(apiUrl).bodyAsText()
            val apiResponse = PeptideJson.decodeFromString<ServersResponse>(responseText)

            if (apiResponse.success) {
                // Map API entries directly to ServerData and preserve order
                val mapped = apiResponse.data.map { entry ->
                    ServerData(
                        id = entry.id,
                        name = entry.name ?: "",
                        description = entry.description ?: "",
                        inviteCode = entry.inviteCode ?: "",
                        disabled = entry.disabled,
                        showColor = entry.showcolor,
                        sortOrder = entry.sortorder
                    )
                }

                emit(mapped)
                return@flow
            }
        } catch (e: Exception) {
            // Any failure -> fallback to Google Sheets (preserve previous behavior)
            e.printStackTrace()
        }

        // Fallback: existing Google Sheets parsing
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
                showColor = rowData["showcolor"],
                sortOrder = rowData["sortorder"]?.toIntOrNull()
            )
        }

        emit(servers.sortedBy { it.sortOrder ?: Int.MAX_VALUE })
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
    val sortOrder: Int? = null,
)