package chat.stoat.internals.server

import chat.stoat.core.model.schemas.Category
import chat.stoat.core.model.schemas.Server

const val UncategorisedChannelSectionId = "default"

data class ServerChannelSection(
    val id: String,
    val title: String?,
    val channelIds: List<String>,
)

sealed interface ServerChannelListEntry {
    val key: String
    val sectionId: String

    data class Section(
        override val sectionId: String,
    ) : ServerChannelListEntry {
        override val key: String = "section:$sectionId"
    }

    data class Channel(
        override val sectionId: String,
        val channelId: String,
    ) : ServerChannelListEntry {
        override val key: String = "channel:$channelId"
    }
}

fun serverChannelSections(server: Server): List<ServerChannelSection> {
    val remaining = LinkedHashSet(server.channels.orEmpty())
    val seenCategoryIds = mutableSetOf<String>()
    var defaultChannels = emptyList<String>()
    val categories = buildList {
        server.categories.orEmpty().forEach { category ->
            val id = category.id ?: return@forEach
            if (!seenCategoryIds.add(id)) return@forEach
            val channels = category.channels.orEmpty().filter(remaining::remove)
            if (id == UncategorisedChannelSectionId) {
                defaultChannels = channels
            } else {
                add(
                    ServerChannelSection(
                        id = id,
                        title = category.title,
                        channelIds = channels,
                    )
                )
            }
        }
    }

    return listOf(
        ServerChannelSection(
            id = UncategorisedChannelSectionId,
            title = null,
            channelIds = defaultChannels + remaining,
        )
    ) + categories
}

fun List<ServerChannelSection>.toServerCategories(): List<Category> = map { section ->
    Category(
        id = section.id,
        title = if (section.id == UncategorisedChannelSectionId) {
            "Default"
        } else {
            section.title.orEmpty()
        },
        channels = section.channelIds,
    )
}

fun List<ServerChannelSection>.flattenChannelSections(): List<ServerChannelListEntry> =
    flatMap { section ->
        listOf(ServerChannelListEntry.Section(section.id)) +
                section.channelIds.map { channelId ->
                    ServerChannelListEntry.Channel(section.id, channelId)
                }
    }

fun moveServerChannelEntry(
    sections: List<ServerChannelSection>,
    fromIndex: Int,
    toIndex: Int,
): List<ServerChannelSection> {
    val entries = sections.flattenChannelSections()
    val moving = entries.getOrNull(fromIndex) ?: return sections
    val target = entries.getOrNull(toIndex) ?: return sections
    if (moving == target) return sections

    return when (moving) {
        is ServerChannelListEntry.Section -> {
            if (moving.sectionId == UncategorisedChannelSectionId) return sections
            val fromSection = sections.indexOfFirst { it.id == moving.sectionId }
            val targetSection = sections.indexOfFirst { it.id == target.sectionId }
            if (fromSection < 1 || targetSection < 0 || fromSection == targetSection) return sections

            sections.toMutableList().apply {
                val section = removeAt(fromSection)
                add(targetSection.coerceAtLeast(1).coerceAtMost(size), section)
            }
        }

        is ServerChannelListEntry.Channel -> {
            val reordered = entries.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
            val byId = sections.associateBy(ServerChannelSection::id)
            val rebuilt = sections.associate { it.id to mutableListOf<String>() }.toMutableMap()
            var currentSectionId = UncategorisedChannelSectionId

            reordered.forEach { entry ->
                when (entry) {
                    is ServerChannelListEntry.Section -> currentSectionId = entry.sectionId
                    is ServerChannelListEntry.Channel -> rebuilt
                        .getOrPut(currentSectionId) { mutableListOf() }
                        .add(entry.channelId)
                }
            }

            sections.map { section ->
                byId.getValue(section.id).copy(channelIds = rebuilt[section.id].orEmpty())
            }
        }
    }
}
