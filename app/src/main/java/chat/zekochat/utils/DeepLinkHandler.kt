package chat.zekochat.utils

/**
 * Static handler for deep link data that needs to be passed between activities and composables.
 */
object DeepLinkHandler {
    /**
     * Channel ID from deep link
     */
    var channelId: String? = null
    
    /**
     * Server ID from deep link
     */
    var serverId: String? = null
    
    /**
     * User ID from deep link
     */
    var userId: String? = null
    
    /**
     * Message ID from deep link
     */
    var messageId: String? = null
    
    /**
     * Flag indicating if there is an active deep link to process
     */
    var hasDeepLink: Boolean = false
    
    /**
     * Clears all deep link data
     */
    fun clear() {
        channelId = null
        serverId = null
        userId = null
        messageId = null
        hasDeepLink = false
    }
}
