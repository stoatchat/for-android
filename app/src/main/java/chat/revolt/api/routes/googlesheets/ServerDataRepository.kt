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
            )
        }
        emit(servers)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Fetches server categories from Google Sheets and returns as a Flow
     * @param sheetUrl The URL of the published Google Sheet (CSV or JSON)
     * @return Flow of ServerCategory list
     */
    fun getServerCategories(sheetUrl: String): Flow<List<ServerCategory>> = flow {
        val categories = GoogleSheetsService.fetchSheetData(
            sheetUrl = sheetUrl
        ) { rowData ->
            ServerCategory(
                id = rowData["id"] ?: "",
                name = rowData["name"] ?: "",
                description = rowData["description"] ?: "",
                iconName = rowData["iconName"] ?: "",
                sortOrder = rowData["sortOrder"]?.toIntOrNull() ?: 0
            )
        }
        emit(categories)
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
)

/**
 * Data class representing a server category
 */
@Serializable
data class ServerCategory(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val sortOrder: Int
)