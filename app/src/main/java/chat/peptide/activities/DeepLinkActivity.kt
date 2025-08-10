package chat.peptide.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import chat.peptide.persistence.KVStorage
import chat.peptide.utils.DeepLinkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeepLinkActivity : AppCompatActivity() {
    
    @Inject
    lateinit var kvStorage: KVStorage

    companion object {
        private const val TAG = "DeepLinkActivity"
        
        // Constants for deep link paths
        const val PATH_CHANNEL = "channel"
        const val PATH_SERVER = "server"
        const val PATH_USER = "user"
        const val PATH_MESSAGE = "message"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle the incoming intent
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        // Handle the new intent
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        val appLinkAction = intent.action
        val appLinkData: Uri? = intent.data
        
        if (Intent.ACTION_VIEW == appLinkAction && appLinkData != null) {
            Log.d(TAG, "Deep link received: $appLinkData")
            
            // Check if this is a valid Revolt deep link
            if (!DeepLinkUtils.isRevoltDeepLink(appLinkData)) {
                Log.d(TAG, "Not a valid Revolt deep link: $appLinkData")
                startMainActivity()
                return
            }
            
            // Check if user is logged in and handle deep link in a coroutine
            lifecycleScope.launch {
                val token = kvStorage.get("sessionToken")
                if (token == null) {
                    // User is not logged in, redirect to login screen
                    Log.d(TAG, "User not logged in, redirecting to login screen")
                    startMainActivity("choose-platform")
                    return@launch
                }
                
                // First, check for server/channel/message format
                val serverChannelMessage = DeepLinkUtils.extractServerChannelMessage(appLinkData)
                if (serverChannelMessage != null) {
                    Log.d(TAG, "Detected server/channel/message format: ${serverChannelMessage.serverId}/${serverChannelMessage.channelId}/${serverChannelMessage.messageId}")
                    
                    if (serverChannelMessage.messageId != null) {
                        // Navigate to the specific message in the channel
                        navigateToMessage(serverChannelMessage.messageId, serverChannelMessage.channelId)
                    } else {
                        // Navigate to the channel in the server
                        navigateToChannel(serverChannelMessage.channelId)
                    }
                    return@launch
                }
                
                // Extract IDs from the URI using our utility class for other formats
                val channelId = DeepLinkUtils.extractChannelId(appLinkData)
                val serverId = DeepLinkUtils.extractServerId(appLinkData)
                val userId = DeepLinkUtils.extractUserId(appLinkData)
                val messageId = DeepLinkUtils.extractMessageId(appLinkData)
                
                when {
                    channelId != null -> {
                        Log.d(TAG, "Navigating to channel: $channelId")
                        navigateToChannel(channelId)
                    }
                    serverId != null -> {
                        Log.d(TAG, "Navigating to server: $serverId")
                        navigateToServer(serverId)
                    }
                    userId != null -> {
                        Log.d(TAG, "Navigating to user: $userId")
                        navigateToUser(userId)
                    }
                    messageId != null -> {
                        Log.d(TAG, "Navigating to message: $messageId")
                        val channelParam = appLinkData.getQueryParameter("channel")
                        navigateToMessage(messageId, channelParam)
                    }
                    else -> {
                        // Default case, just open the main activity
                        Log.d(TAG, "No specific path, opening main activity")
                        startMainActivity()
                    }
                }
            }
        } else {
            // Not a deep link, just open the main activity
            Log.d(TAG, "Not a deep link, opening main activity")
            startMainActivity()
        }
    }
    
    private fun navigateToChannel(channelId: String) {
        Log.d(TAG, "Navigating to channel: $channelId")
        
        // Use our utility class to create the intent
        val intent = DeepLinkUtils.createChannelIntent(this, channelId)
        startActivity(intent)
        finish()
    }
    
    private fun navigateToServer(serverId: String) {
        Log.d(TAG, "Navigating to server: $serverId")
        
        // Use our utility class to create the intent
        val intent = DeepLinkUtils.createServerIntent(this, serverId)
        startActivity(intent)
        finish()
    }
    
    private fun navigateToUser(userId: String) {
        Log.d(TAG, "Navigating to user: $userId")
        
        // Use our utility class to create the intent
        val intent = DeepLinkUtils.createUserIntent(this, userId)
        startActivity(intent)
        finish()
    }
    
    private fun navigateToMessage(messageId: String, channelId: String?) {
        Log.d(TAG, "Navigating to message: $messageId in channel: $channelId")
        
        // Use our utility class to create the intent
        val intent = DeepLinkUtils.createMessageIntent(this, messageId, channelId)
        startActivity(intent)
        finish()
    }
    
    private fun startMainActivity(destination: String? = null) {
        val intent = Intent(this, MainActivity::class.java)
        if (destination != null) {
            intent.putExtra("next_destination", destination)
        }
        startActivity(intent)
        finish()
    }
}
