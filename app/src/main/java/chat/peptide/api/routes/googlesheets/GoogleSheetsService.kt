package chat.peptide.api.routes.googlesheets

import chat.peptide.api.RevoltHttp
import chat.peptide.api.RevoltJson
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service for fetching and parsing Google Sheets data
 * This supports both JSON and CSV formats from published Google Sheets
 */
object GoogleSheetsService {

    /**
     * Fetches data from a Google Sheet and maps it to a list of objects of type T
     * @param sheetUrl The URL of the published Google Sheet (CSV or JSON)
     * @param mapper A function that maps a row (as a Map<String, String>) to an object of type T
     * @return A list of objects of type T
     */
    suspend inline fun <reified T> fetchSheetData(
        sheetUrl: String,
        crossinline mapper: (Map<String, String>) -> T
    ): List<T> = withContext(Dispatchers.IO) {
        try {
            val response = RevoltHttp.get(sheetUrl).bodyAsText()
            
            // Determine if the response is CSV or JSON
            if (sheetUrl.contains("output=csv") || response.startsWith("\"") || response.contains(",")) {
                parseCSV(response, mapper)
            } else {
                parseJSON(response, mapper)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Parse CSV data from Google Sheets
     */
    inline fun <reified T> parseCSV(
        csvData: String,
        crossinline mapper: (Map<String, String>) -> T
    ): List<T> {
        // Split the CSV into lines
        val lines = csvData.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        
        // Parse header row
        val headers = lines[0].split(",").map { 
            it.trim().removeSurrounding("\"") 
        }
        
        // Parse data rows
        return lines.drop(1).map { line ->
            val values = parseCSVLine(line)
            val rowData = mutableMapOf<String, String>()
            
            headers.forEachIndexed { index, header ->
                if (index < values.size) {
                    rowData[header] = values[index]
                }
            }
            
            mapper(rowData)
        }
    }
    
    /**
     * Parse a single CSV line, handling quoted values properly
     */
    fun parseCSVLine(line: String): List<String> {
        val values = mutableListOf<String>()
        var currentValue = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '\"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    values.add(currentValue.toString().trim().removeSurrounding("\""))
                    currentValue = StringBuilder()
                }
                else -> currentValue.append(char)
            }
        }
        
        // Add the last value
        values.add(currentValue.toString().trim().removeSurrounding("\""))
        
        return values
    }

    /**
     * Parse JSON data from Google Sheets
     */
    inline fun <reified T> parseJSON(
        jsonData: String,
        crossinline mapper: (Map<String, String>) -> T
    ): List<T> {
        val sheetResponse = RevoltJson.decodeFromString<GoogleSheetResponse>(jsonData)
        
        // Convert the sheet data to a list of objects
        val headers = sheetResponse.feed.entry
            .take(sheetResponse.feed.entry.firstOrNull()?.content?.columnCount ?: 0)
            .mapIndexed { index, entry -> 
                "gsx\$column${index + 1}" to entry.title.text 
            }.toMap()
        
        val rows = sheetResponse.feed.entry.chunked(headers.size)
            .drop(1) // Skip header row
            .map { rowEntries ->
                val rowData = mutableMapOf<String, String>()
                rowEntries.forEachIndexed { index, entry ->
                    val columnName = headers["gsx\$column${index + 1}"] ?: "column${index + 1}"
                    rowData[columnName] = entry.content.text
                }
                mapper(rowData)
            }
        
        return rows
    }
}

/**
 * Data classes for parsing Google Sheets API response
 */
@Serializable
data class GoogleSheetResponse(
    val feed: Feed
)

@Serializable
data class Feed(
    val entry: List<Entry>
)

@Serializable
data class Entry(
    val title: Title,
    val content: Content
)

@Serializable
data class Title(
    @SerialName("\$t")
    val text: String
)

@Serializable
data class Content(
    @SerialName("\$t")
    val text: String,
    @SerialName("columnCount")
    val columnCount: Int = 0
) 