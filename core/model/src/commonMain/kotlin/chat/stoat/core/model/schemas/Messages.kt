package chat.stoat.core.model.schemas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    @SerialName("_id")
    val id: String? = null,
    val nonce: String? = null,
    val channel: String? = null,
    val author: String? = null,
    val content: String? = null,
    val reactions: Map<String, List<String>>? = null,
    val replies: List<String>? = null,
    val attachments: List<AutumnResource>? = null,
    val edited: String? = null,
    val embeds: List<Embed>? = null,
    val mentions: List<String>? = null,
    val masquerade: Masquerade? = null,
    val system: SystemInfo? = null,
    val webhook: WebHook? = null,
    val interactions: InteractionsDescription? = null,
    val pinned: Boolean? = null,
    /**
     * See [chat.stoat.api.internals.MessageFlag]
     */
    val flags: Int? = null,
    val type: String? = null, // this is _only_ used for websocket events!
    val tail: Boolean? = null // this is used to determine if the message is the last in a message group
) {

    fun mergeWithPartial(partial: Message): Message {
        return Message(
            id = partial.id ?: id,
            nonce = partial.nonce ?: nonce,
            channel = partial.channel ?: channel,
            author = partial.author ?: author,
            content = partial.content ?: content,
            reactions = partial.reactions ?: reactions,
            replies = partial.replies ?: replies,
            attachments = partial.attachments ?: attachments,
            edited = partial.edited ?: edited,
            embeds = partial.embeds ?: embeds,
            mentions = partial.mentions ?: mentions,
            masquerade = partial.masquerade ?: masquerade,
            type = partial.type ?: type,
            tail = partial.tail ?: tail
        )
    }
}

@Serializable
data class Embed(
    val type: String? = null,
    val url: String? = null,

    @SerialName("original_url")
    val originalURL: String? = null,

    val special: Special? = null,
    val title: String? = null,
    val description: String? = null,
    val image: Image? = null,

    @SerialName("icon_url")
    val iconURL: String? = null,

    @SerialName("site_name")
    val siteName: String? = null,

    val colour: String? = null,
    val width: Long? = null,
    val height: Long? = null,
    val size: String? = null
)

@Serializable
data class Image(
    val url: String? = null,
    val width: Long? = null,
    val height: Long? = null,
    val size: String? = null
)

@Serializable
data class Special(
    val type: String? = null,
    val id: String? = null,
    val timestamp: String? = null,
    @SerialName("content_type")
    val contentType: String? = null,
    @SerialName("album_id")
    val albumID: String? = null,
    @SerialName("track_id")
    val trackID: String? = null,
)

@Serializable
data class Masquerade(
    val name: String? = null,
    val avatar: String? = null,
    val colour: String? = null
)

@Serializable
data class SystemInfo(
    val type: String? = null,
    val id: String? = null,
    val name: String? = null,
    val by: String? = null,
    val from: String? = null,
    val to: String? = null,
    val content: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
)

@Serializable
data class WebHook(
    val avatar: String? = null,
    val name: String? = null,
)

@Serializable
data class InteractionsDescription(
    val reactions: List<String>? = null,
    @SerialName("restrict_reactions") val restrictReactions: Boolean? = null
)