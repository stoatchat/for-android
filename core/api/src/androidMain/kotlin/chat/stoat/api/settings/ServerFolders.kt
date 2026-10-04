package chat.stoat.api.settings

import chat.stoat.api.internals.ULID
import chat.stoat.core.model.schemas.OrderingSettings
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.ServerFolder
import chat.stoat.core.model.schemas.ServerFoldersSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

private const val FOLDER_PREFIX = "folder-"

sealed interface ServerSidebarEntry {
    val id: String

    data class Single(val server: Server) : ServerSidebarEntry {
        override val id: String get() = server.id!!
    }

    data class Folder(val folder: ServerFolder, val servers: List<Server>) : ServerSidebarEntry {
        override val id: String get() = folder.id
    }
}

private val JsonElement.stringOrNull: String?
    get() = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

/**
 * Mirrors Stoat for Web `Ordering.clean`
 */
internal fun cleanOrdering(input: JsonElement): OrderingSettings {
    val obj = input as? JsonObject ?: throw IllegalArgumentException("ordering is not an object")

    fun cleanIds(element: JsonElement?): List<String> {
        val seen = LinkedHashSet<String>()
        (element as? JsonArray)?.forEach { entry ->
            val ids = when (entry) {
                is JsonPrimitive -> listOf(entry)
                is JsonObject -> entry["servers"] as? JsonArray ?: emptyList()
                else -> emptyList()
            }
            ids.mapNotNull { it.stringOrNull }
                .filter { it.isNotEmpty() }
                .forEach { seen.add(it) }
        }
        return seen.toList()
    }

    return OrderingSettings(
        servers = cleanIds(obj["servers"]),
        serverSidebar = (obj["serverSidebar"] as? JsonArray)?.let { cleanIds(it) },
    )
}

/**
 * A server may only belong to the first folder claiming it
 * Mirrors Stoat for Web `Ordering.clean`
 */
internal fun cleanServerFolders(input: JsonElement): ServerFoldersSettings {
    val obj = input as? JsonObject
        ?: throw IllegalArgumentException("server-folders is not an object")

    val seenFolders = mutableSetOf<String>()
    val seenServers = mutableSetOf<String>()

    val folders = (obj["folders"] as? JsonArray).orEmpty().mapNotNull { entry ->
        val folder = entry as? JsonObject ?: return@mapNotNull null
        val id = folder["id"]?.stringOrNull?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        if (!seenFolders.add(id)) return@mapNotNull null

        ServerFolder(
            id = id,
            name = folder["name"]?.stringOrNull ?: "",
            colour = folder["colour"]?.stringOrNull,
            collapsed = (folder["collapsed"] as? JsonPrimitive)?.booleanOrNull?.takeIf { it },
            servers = (folder["servers"] as? JsonArray).orEmpty()
                .mapNotNull { it.stringOrNull }
                .filter { seenServers.add(it) },
        )
    }

    return ServerFoldersSettings(folders)
}

/**
 * Port of Stoat for Web `Ordering.orderedEntries`.
 */
fun resolveServerSidebar(
    servers: Map<String, Server>,
    ordering: OrderingSettings,
    folders: List<ServerFolder>,
): List<ServerSidebarEntry> {
    val known = servers.keys.toMutableSet()
    val byId = folders.associateBy { it.id }
    val folderOf = mutableMapOf<String, ServerFolder>()
    folders.forEach { folder ->
        folder.servers.forEach { folderOf.putIfAbsent(it, folder) }
    }

    val out = mutableListOf<ServerSidebarEntry>()
    val drawn = mutableSetOf<String>()

    fun take(id: String) {
        val folder = byId[id] ?: folderOf[id]
        if (folder != null) {
            if (!drawn.add(folder.id)) return
            val members = folder.servers.filter { known.remove(it) }.mapNotNull { servers[it] }
            if (members.isNotEmpty()) {
                out += ServerSidebarEntry.Folder(folder, members)
            }
            return
        }

        if (known.remove(id)) {
            servers[id]?.let { out += ServerSidebarEntry.Single(it) }
        }
    }

    (ordering.serverSidebar ?: ordering.servers).forEach(::take)
    known.sorted().forEach(::take)

    return out
}

object ServerFolders {
    val folders: List<ServerFolder>
        get() = SyncedSettings.serverFolders.folders

    fun folderOf(serverId: String): ServerFolder? =
        folders.firstOrNull { serverId in it.servers }

    fun create(name: String, serverIds: List<String>): String {
        val id = newFolderId()
        commit(
            without(folders, serverIds.toSet()) + ServerFolder(
                id = id,
                name = name,
                servers = serverIds
            )
        )
        return id
    }

    fun edit(id: String, name: String, colour: String?) {
        commit(folders.map { if (it.id == id) it.copy(name = name, colour = colour) else it })
    }

    fun remove(id: String) {
        commit(folders.filterNot { it.id == id })
    }

    fun toggle(id: String) {
        commit(
            folders.map {
                if (it.id == id) it.copy(collapsed = if (it.collapsed == true) null else true) else it
            }
        )
    }

    fun addServer(folderId: String, serverId: String) {
        if (folders.any { it.id == folderId && serverId in it.servers }) return
        commit(placed(folders, folderId, serverId, before = null))
    }

    fun removeServer(serverId: String) {
        commit(without(folders, setOf(serverId)))
    }

    /**
     * Port of Stoat for Web `fold` (`ServerList.tsx`).
     */
    fun fold(
        entries: List<ServerSidebarEntry>,
        target: String,
        incoming: String,
        newFolderName: String,
    ) {
        val entry = entries.firstOrNull { it.id == target } ?: return
        val ids = entries.map { it.id } - incoming

        if (entry is ServerSidebarEntry.Folder) {
            commit(placed(folders, entry.id, incoming, before = null), ids)
            return
        }

        val id = newFolderId()
        commit(
            without(folders, setOf(target, incoming)) +
                    ServerFolder(id = id, name = newFolderName, servers = listOf(target, incoming)),
            ids.map { if (it == target) id else it }
        )
    }

    /**
     * Port of Stoat for Web `place` (`ServerList.tsx`).
     */
    fun move(
        entries: List<ServerSidebarEntry>,
        moved: String,
        before: String?,
        parent: String?,
    ) {
        val currentOrder = entries.map { it.id }
        val ids = currentOrder - moved

        if (parent != null) {
            commit(placed(folders, parent, moved, before), ids)
            return
        }

        val updatedFolders = folderOf(moved)?.let { without(folders, setOf(moved)) }
        val index =
            if (before == null) ids.size else ids.indexOf(before).takeIf { it >= 0 } ?: return
        val newOrder = ids.subList(0, index) + moved + ids.subList(index, ids.size)

        if (updatedFolders == null && newOrder == currentOrder) return
        commit(updatedFolders, newOrder)
    }

    private fun newFolderId() = FOLDER_PREFIX + ULID.makeNext()

    private fun without(folders: List<ServerFolder>, serverIds: Set<String>) =
        folders.map { folder -> folder.copy(servers = folder.servers.filterNot { it in serverIds }) }

    private fun placed(
        folders: List<ServerFolder>,
        folderId: String,
        serverId: String,
        before: String?,
    ) = folders.map { folder ->
        val servers = folder.servers - serverId
        if (folder.id != folderId) return@map folder.copy(servers = servers)

        val index = before?.let { servers.indexOf(it) }?.takeIf { it >= 0 } ?: servers.size
        folder.copy(
            servers = servers.subList(0, index) + serverId + servers.subList(
                index,
                servers.size
            )
        )
    }

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun commit(folders: List<ServerFolder>?, order: List<String>? = null) {
        val folderSettings = folders?.let { list ->
            ServerFoldersSettings(list.filter { it.servers.isNotEmpty() })
        }
        val ordering = order?.let { ids ->
            val byId = (folderSettings?.folders ?: this.folders).associateBy { it.id }
            SyncedSettings.ordering.copy(
                serverSidebar = ids.flatMap { id ->
                    byId[id]?.let { listOf(id) + it.servers } ?: listOf(id)
                }
            )
        }

        // undispatched so local state is updated before this returns
        folderSettings?.let {
            syncScope.launch(start = CoroutineStart.UNDISPATCHED) {
                syncing { SyncedSettings.updateServerFolders(it) }
            }
        }
        ordering?.let {
            syncScope.launch(start = CoroutineStart.UNDISPATCHED) {
                syncing { SyncedSettings.updateOrdering(it) }
            }
        }
    }

    private suspend fun syncing(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "Failed to sync server sidebar: " + e.asLog() }
        }
    }
}
