package chat.revolt.internals

import android.content.Context
import chat.revolt.api.REVOLT_KJBOOK
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.internals.IndexHolder.cachedIndex
import chat.revolt.persistence.KVStorage
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable

@Serializable
data class ChangelogIndex(
    val changelogs: List<ChangelogData> = emptyList()
)

@Serializable
data class ChangelogData(
    val version: ChangelogVersion,
    val date: ChangelogDate,
    val summary: String
)

@Serializable
data class ChangelogDate(
    val publish: String
)

@Serializable
data class ChangelogVersion(
    val code: Long,
    val name: String,
    val title: String
)

@Serializable
data class Changelog(
    val id: String,
    val slug: String,
    val body: String,
    val collection: String,
    val data: ChangelogData,
    val rendered: String
)

object IndexHolder {
    var cachedIndex: ChangelogIndex? = null
}

class Changelogs(val context: Context, val kvStorage: KVStorage? = null) {
    suspend fun fetchChangelogIndex(): ChangelogIndex {
        if (cachedIndex != null) {
            return cachedIndex as ChangelogIndex
        }

        try {
            val response = RevoltHttp.get("$REVOLT_KJBOOK/changelogs.json")
            cachedIndex =
                RevoltJson.decodeFromString(ChangelogIndex.serializer(), response.bodyAsText())
            return cachedIndex as ChangelogIndex
        } catch (e: Error) {
            return ChangelogIndex()
        }
    }

    suspend fun fetchChangelogByVersionCode(versionCode: Long): Changelog {
        try {
            val response = RevoltHttp.get("$REVOLT_KJBOOK/changelogs/$versionCode.json")
            return RevoltJson.decodeFromString(Changelog.serializer(), response.bodyAsText())
        } catch (e: Error) {
            return Changelog(
                id = "",
                slug = "",
                body = e.localizedMessage ?: "",
                collection = "",
                data = ChangelogData(
                    version = ChangelogVersion(
                        code = 0L,
                        name = "",
                        title = e.localizedMessage ?: "",
                    ),
                    date = ChangelogDate(
                        publish = "1970-01-01T00:00:00.000Z"
                    ),
                    summary = e.localizedMessage ?: ""
                ),
                rendered = e.localizedMessage ?: ""
            )
        }
    }

    suspend fun getLatestChangelog(): ChangelogData {
        return fetchChangelogIndex().changelogs.maxByOrNull { it.version.code }!!
    }

    suspend fun getLatestChangelogCode(): String {
        return getLatestChangelog().version.code.toString()
    }

    suspend fun hasSeenCurrent(): Boolean {
        if (kvStorage == null) {
            throw IllegalStateException(
                "Not supported for non-KVStorage instances of Changelogs"
            )
        }

        val latest = getLatestChangelog().version.code
        val lastRead = kvStorage.get("latestChangelogRead")

        if (lastRead == null) {
            return false
        }

        // If the last read changelog is >= the latest, it has been read
        return lastRead.toLong() >= latest
    }

    suspend fun markAsSeen() {
        if (kvStorage == null) {
            throw IllegalStateException(
                "Not supported for non-KVStorage instances of Changelogs"
            )
        }

        val index = fetchChangelogIndex()
        val latest = index.changelogs.maxByOrNull { it.version.code }!!.version.code.toString()
        kvStorage.set("latestChangelogRead", latest)
    }
}
