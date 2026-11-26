package chat.stoat.composables.markdown.prose

/** Actions for when you tap a mention, emote or link in [ChatMarkdown]. This is injected using DI */
interface ChatMarkdownActions {
    suspend fun openUser(userId: String, serverId: String?)
    suspend fun openChannel(channelId: String)
    suspend fun openEmoteInfo(emoteId: String)

    /**
     * Opens [uri] inside the app if it's a link the app handles internally, such as a Stoat message
     * or a server invite.
     * If it returns false the link should be opened in the browser instead and is not handled.
     */
    suspend fun openInAppLink(uri: String): Boolean
}
