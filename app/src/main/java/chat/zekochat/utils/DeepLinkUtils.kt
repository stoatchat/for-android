package chat.zekochat.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import chat.zekochat.activities.DeepLinkActivity
import chat.zekochat.activities.MainActivity

/**
 * Utility class for handling deep links in the Peptide app.
 */
object DeepLinkUtils {

    /**
     * Checks if the given URI is a valid Peptide deep link.
     *
     * @param uri The URI to check
     * @return true if the URI is a valid Peptide deep link, false otherwise
     */
    fun isPeptideDeepLink(uri: Uri?): Boolean {
        if (uri == null) return false
        
        val host = uri.host ?: return false
        val scheme = uri.scheme ?: return false
        
        // Check if the scheme is http or https
        if (scheme != "http" && scheme != "https") return false
        
        // Check if the host is a valid Peptide host
        return host == "peptide.chat" || host == "app.peptide.chat"
    }
    
    /**
     * Extracts the channel ID from a channel deep link URI.
     *
     * @param uri The URI to extract the channel ID from
     * @return The channel ID, or null if the URI is not a valid channel deep link
     */
    fun extractChannelId(uri: Uri?): String? {
        if (uri == null) return null
        
        val pathSegments = uri.pathSegments
        
        // Handle /channels/CHANNEL_ID format
        if (pathSegments.size >= 2 && pathSegments[0] == "channels") {
            return pathSegments[1]
        }
        
        // Handle /server/SERVER_ID/channel/CHANNEL_ID format
        if (pathSegments.size >= 4 && 
            pathSegments[0] == "server" && 
            pathSegments[2] == "channel") {
            return pathSegments[3]
        }
        
        return null
    }
    
    /**
     * Extracts the server ID from a server deep link URI.
     *
     * @param uri The URI to extract the server ID from
     * @return The server ID, or null if the URI is not a valid server deep link
     */
    fun extractServerId(uri: Uri?): String? {
        if (uri == null) return null
        
        val pathSegments = uri.pathSegments
        
        // Handle /servers/SERVER_ID format
        if (pathSegments.size >= 2 && pathSegments[0] == "servers") {
            return pathSegments[1]
        }
        
        // Handle /server/SERVER_ID format
        if (pathSegments.size >= 2 && pathSegments[0] == "server") {
            return pathSegments[1]
        }
        
        return null
    }
    
    /**
     * Extracts the user ID from a user deep link URI.
     *
     * @param uri The URI to extract the user ID from
     * @return The user ID, or null if the URI is not a valid user deep link
     */
    fun extractUserId(uri: Uri?): String? {
        if (uri == null) return null
        
        val pathSegments = uri.pathSegments
        if (pathSegments.size < 2 || pathSegments[0] != "users") return null
        
        return pathSegments[1]
    }
    
    /**
     * Extracts the message ID from a message deep link URI.
     *
     * @param uri The URI to extract the message ID from
     * @return The message ID, or null if the URI is not a valid message deep link
     */
    fun extractMessageId(uri: Uri?): String? {
        if (uri == null) return null
        
        val pathSegments = uri.pathSegments
        
        // Handle /messages/MESSAGE_ID format
        if (pathSegments.size >= 2 && pathSegments[0] == "messages") {
            return pathSegments[1]
        }
        
        // Handle /server/SERVER_ID/channel/CHANNEL_ID/MESSAGE_ID format
        if (pathSegments.size >= 5 && 
            pathSegments[0] == "server" && 
            pathSegments[2] == "channel") {
            return pathSegments[4]
        }
        
        return null
    }
    
    /**
     * Data class to hold server, channel, and message IDs extracted from a URI.
     */
    data class ServerChannelMessage(
        val serverId: String,
        val channelId: String,
        val messageId: String? = null
    )
    
    /**
     * Extracts server, channel, and message IDs from a server channel message deep link URI.
     *
     * @param uri The URI to extract the IDs from
     * @return A ServerChannelMessage object containing the extracted IDs, or null if the URI is not a valid server channel message deep link
     */
    fun extractServerChannelMessage(uri: Uri?): ServerChannelMessage? {
        if (uri == null) return null
        
        val pathSegments = uri.pathSegments
        
        // Handle /server/SERVER_ID/channel/CHANNEL_ID format
        if (pathSegments.size >= 4 && 
            pathSegments[0] == "server" && 
            pathSegments[2] == "channel") {
            
            val serverId = pathSegments[1]
            val channelId = pathSegments[3]
            
            // Check if there's a message ID (5th segment)
            val messageId = if (pathSegments.size >= 5) pathSegments[4] else null
            
            return ServerChannelMessage(serverId, channelId, messageId)
        }
        
        return null
    }
    
    /**
     * Creates an intent to navigate to a specific channel.
     *
     * @param context The context to use for creating the intent
     * @param channelId The ID of the channel to navigate to
     * @return An intent that will navigate to the specified channel
     */
    fun createChannelIntent(context: Context, channelId: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra(DeepLinkActivity.PATH_CHANNEL, channelId)
        }
    }
    
    /**
     * Creates an intent to navigate to a specific server.
     *
     * @param context The context to use for creating the intent
     * @param serverId The ID of the server to navigate to
     * @return An intent that will navigate to the specified server
     */
    fun createServerIntent(context: Context, serverId: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra(DeepLinkActivity.PATH_SERVER, serverId)
        }
    }
    
    /**
     * Creates an intent to navigate to a specific user.
     *
     * @param context The context to use for creating the intent
     * @param userId The ID of the user to navigate to
     * @return An intent that will navigate to the specified user
     */
    fun createUserIntent(context: Context, userId: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra(DeepLinkActivity.PATH_USER, userId)
        }
    }
    
    /**
     * Creates an intent to navigate to a specific message.
     *
     * @param context The context to use for creating the intent
     * @param messageId The ID of the message to navigate to
     * @param channelId The ID of the channel containing the message, if known
     * @return An intent that will navigate to the specified message
     */
    fun createMessageIntent(context: Context, messageId: String, channelId: String? = null): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra(DeepLinkActivity.PATH_MESSAGE, messageId)
            if (channelId != null) {
                putExtra(DeepLinkActivity.PATH_CHANNEL, channelId)
            }
        }
    }
}
