package chat.zekochat.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import chat.zekochat.activities.DeepLinkActivity
import chat.zekochat.callbacks.Action
import chat.zekochat.callbacks.ActionChannel
import chat.zekochat.screens.chat.ChatRouterDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility class to handle internal links within the app
 */
object InternalLinkHandler {
    private const val TAG = "InternalLinkHandler"
    
    /**
     * Checks if a URL is an internal link that should be handled by the app
     * 
     * @param url The URL to check
     * @return true if the URL is an internal link, false otherwise
     */
    fun isInternalLink(url: String): Boolean {
        val uri = Uri.parse(url)
        val isInternal = DeepLinkUtils.isPeptideDeepLink(uri)
        Log.d(TAG, "isInternalLink: $url -> $isInternal")
        return isInternal
    }
    
    /**
     * Handles an internal link by either navigating within the app or launching the DeepLinkActivity
     * 
     * @param context The context to use for launching activities
     * @param url The URL to handle
     * @param isAlreadyInChatScreen Whether the user is already in the chat screen
     * @return true if the link was handled, false otherwise
     */
    fun handleInternalLink(context: Context, url: String, isAlreadyInChatScreen: Boolean = false): Boolean {
        Log.d(TAG, "handleInternalLink called with url: $url, isAlreadyInChatScreen: $isAlreadyInChatScreen")
        
        if (!isInternalLink(url)) {
            Log.d(TAG, "Not an internal link, returning false")
            return false
        }
        
        Log.d(TAG, "Handling internal link: $url")
        val uri = Uri.parse(url)
        
        // If we're already in the chat screen, we can use the ActionChannel to navigate
        if (isAlreadyInChatScreen) {
            Log.d(TAG, "Already in chat screen, using ActionChannel")
            val result = handleLinkInChatScreen(uri)
            Log.d(TAG, "handleLinkInChatScreen result: $result")
            return result
        }
        
        // Otherwise, launch the DeepLinkActivity
        try {
            Log.d(TAG, "Launching DeepLinkActivity")
            val intent = Intent(Intent.ACTION_VIEW, uri, context, DeepLinkActivity::class.java)
            context.startActivity(intent)
            Log.d(TAG, "DeepLinkActivity launched successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching DeepLinkActivity", e)
            return false
        }
    }
    
    /**
     * Handles a link when already in the chat screen by sending actions through the ActionChannel
     * 
     * @param uri The URI to handle
     * @return true if the link was handled, false otherwise
     */
    private fun handleLinkInChatScreen(uri: Uri): Boolean {
        Log.d(TAG, "handleLinkInChatScreen called with uri: $uri")
        
        try {
            // Extract data from the URI
            val serverChannelMessage = DeepLinkUtils.extractServerChannelMessage(uri)
            Log.d(TAG, "serverChannelMessage: $serverChannelMessage")
            
            if (serverChannelMessage != null) {
                // Handle server/channel/message format
                val serverId = serverChannelMessage.serverId
                val channelId = serverChannelMessage.channelId
                val messageId = serverChannelMessage.messageId
                
                Log.d(TAG, "Handling server/channel/message: $serverId/$channelId/$messageId")
                
                // First navigate to the channel
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        Log.d(TAG, "Sending SwitchChannel action for channelId: $channelId")
                        ActionChannel.send(Action.SwitchChannel(channelId,messageId))
                        Log.d(TAG, "SwitchChannel action sent successfully")
                        
                        // TODO: If messageId is not null, add logic to scroll to the message
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending action", e)
                    }
                }
                
                return true
            }
            
            // Handle channel deep link
            val channelId = DeepLinkUtils.extractChannelId(uri)
            Log.d(TAG, "extractChannelId result: $channelId")
            
            if (channelId != null) {
                Log.d(TAG, "Handling channel: $channelId")
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        Log.d(TAG, "Sending SwitchChannel action for channelId: $channelId")
                        ActionChannel.send(Action.SwitchChannel(channelId))
                        Log.d(TAG, "SwitchChannel action sent successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending action", e)
                    }
                }
                return true
            }
            
            // Handle server deep link
            val serverId = DeepLinkUtils.extractServerId(uri)
            Log.d(TAG, "extractServerId result: $serverId")
            
            if (serverId != null) {
                Log.d(TAG, "Handling server: $serverId")
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        Log.d(TAG, "Sending ChatNavigate action for serverId: $serverId")
                        ActionChannel.send(Action.ChatNavigate(ChatRouterDestination.ServersChannels(serverId)))
                        Log.d(TAG, "ChatNavigate action sent successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending action", e)
                    }
                }
                return true
            }
            
            Log.d(TAG, "No handlers matched for this URI")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleLinkInChatScreen", e)
            return false
        }
    }
}
