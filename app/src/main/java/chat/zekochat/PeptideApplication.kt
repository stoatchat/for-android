package chat.zekochat

import android.app.Application
import android.os.Looper
import android.util.Log
import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.routes.googlesheets.ServerDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import chat.zekochat.api.realtime.DisconnectionState
import chat.zekochat.api.realtime.RealtimeSocket
import chat.zekochat.persistence.KVStorage
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import java.io.IOException
import java.net.SocketException

@HiltAndroidApp
class PeptideApplication : Application() {
    companion object {
        lateinit var instance: PeptideApplication
    }

    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)
        installNetworkUncaughtExceptionHandler()
        PeptideHttp // Trigger initialization
        // Background prefetch for discover servers: only fetch if cache older than 10 minutes.
        try {
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scope.launch {
                try {
                    val repo = ServerDataRepository()
                    val lastTs = try { repo.getLastFetchTs() } catch (_: Exception) { null }
                    val now = System.currentTimeMillis()
                    val STALE_MS = 10 * 60 * 1000L // 10 minutes
                    if (lastTs == null || (now - lastTs >= STALE_MS)) {
                        val csvUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vRY41D-NgTE6bC3kTN3dRpisI-DoeHG8Eg7n31xb1CdydWjOLaphqYckkTiaG9oIQSWP92h3NE-7cpF/pub?gid=0&single=true&output=csv"
                        // Collect first emission to trigger repository fetch and cache save.
                        repo.getServers(csvUrl).first()
                    }
                } catch (_: Exception) {
                    // best-effort prefetch; ignore failures
                }
            }
        } catch (_: Exception) {
            // ignore
        }
    }

    /**
     * Prevents app crashes when OkHttp's WebSocket reader thread throws uncaught
     * network exceptions (e.g. SocketException: Software caused connection abort)
     * that are not propagated to our coroutine try/catch in connectWS().
     */
    private fun installNetworkUncaughtExceptionHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isNetworkRelatedFromBackgroundThread(thread, throwable)) {
                Log.w(
                    "PeptideApplication",
                    "Suppressing uncaught network exception from background thread (e.g. WebSocket connection abort): ${throwable.message}",
                    throwable
                )
                try {
                    if (isWebSocketRelated(throwable)) {
                        RealtimeSocket.updateDisconnectionState(DisconnectionState.Disconnected)
                    }
                } catch (_: Throwable) {
                    // Avoid double-fault when updating state
                }
                return@setDefaultUncaughtExceptionHandler
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun isNetworkRelatedFromBackgroundThread(thread: Thread, throwable: Throwable): Boolean {
        if (thread == Looper.getMainLooper().thread) return false
        var t: Throwable? = throwable
        while (t != null) {
            when (t) {
                is SocketException -> return true
                is IOException -> return true
                else -> t = t.cause
            }
        }
        return false
    }

    private fun isWebSocketRelated(throwable: Throwable): Boolean {
        val stack = throwable.stackTraceToString()
        return stack.contains("okhttp3.internal.ws") ||
            stack.contains("WebSocketReader") ||
            stack.contains("RealWebSocket")
    }

    init {
        instance = this
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}

/**
 * Extension function to get KVStorage instance.
 * @return KVStorage instance
 */
fun PeptideApplication.getKVStorage(): KVStorage {
    return KVStorage(this)
}
