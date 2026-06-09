package chat.zekochat.api.routes.googlesheets

import chat.zekochat.PeptideApplication
import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import chat.zekochat.persistence.KVStorage
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/**
 * Repository for fetching server data. Tries the Public Servers API first,
 * falls back to Google Sheets CSV parsing on any failure.
 */
class ServerDataRepository {

    private val CACHE_KEY = "discover/servers_cache"
    private val CACHE_TS_KEY = "discover/servers_cache_ts"

    // Read cached servers from KVStorage (DataStore). Returns null on parse error or missing
    suspend fun getCachedServers(): List<ServerData>? {
        try {
            val kv = KVStorage(PeptideApplication.instance)
            val json = kv.get(CACHE_KEY) ?: return null
            return PeptideJson.decodeFromString(ListSerializer(ServerData.serializer()), json)
        } catch (_: Exception) {
            return null
        }
    }

    // Save servers list and update timestamp
    suspend fun saveServersToCache(servers: List<ServerData>) {
        try {
            val kv = KVStorage(PeptideApplication.instance)
            val json = PeptideJson.encodeToString(ListSerializer(ServerData.serializer()), servers)
            kv.set(CACHE_KEY, json)
            kv.set(CACHE_TS_KEY, System.currentTimeMillis().toString())
        } catch (_: Exception) {
            // best-effort cache; ignore failures
        }
    }

    suspend fun getLastFetchTs(): Long? {
        try {
            val kv = KVStorage(PeptideApplication.instance)
            return kv.get(CACHE_TS_KEY)?.toLongOrNull()
        } catch (_: Exception) {
            return null
        }
    }

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

                // Update cache (best-effort) then emit
                try {
                    saveServersToCache(mapped)
                } catch (_: Exception) {}

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

        val sorted = servers.sortedBy { it.sortOrder ?: Int.MAX_VALUE }
        try {
            saveServersToCache(sorted)
        } catch (_: Exception) {}

        emit(sorted)
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