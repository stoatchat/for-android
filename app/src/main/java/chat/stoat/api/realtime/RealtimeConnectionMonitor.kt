package chat.stoat.api.realtime

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import chat.stoat.api.StoatAPI
import logcat.logcat
import kotlin.time.Duration.Companion.milliseconds

/**
 * Restarts realtime networking when the app returns into foregroung or we get moved to a different
 * network
 */
internal class RealtimeConnectionMonitor(context: Context) : DefaultLifecycleObserver {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var backgroundedAtMillis: Long? = null
    private var validatedNetwork: Network? = null
    private var hasSeenValidatedNetwork = false
    private var reconnectWhenValidated = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            val isValidated = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )
            if (!isValidated) {
                if (validatedNetwork == network) {
                    validatedNetwork = null
                    reconnectWhenValidated = hasSeenValidatedNetwork
                }
                return
            }

            val previousNetwork = validatedNetwork
            val shouldReconnect = hasSeenValidatedNetwork &&
                    (reconnectWhenValidated || previousNetwork != network)
            validatedNetwork = network
            hasSeenValidatedNetwork = true
            reconnectWhenValidated = false
            if (shouldReconnect) {
                logcat { "Validated default network changed; restarting realtime connection." }
                StoatAPI.requestReconnect("validated default network changed")
            }
        }

        override fun onLost(network: Network) {
            if (validatedNetwork == network) {
                validatedNetwork = null
                reconnectWhenValidated = hasSeenValidatedNetwork
            }
        }
    }

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    override fun onStart(owner: LifecycleOwner) {
        val backgroundedAt = backgroundedAtMillis ?: return
        backgroundedAtMillis = null
        StoatAPI.onAppForegrounded(
            (SystemClock.elapsedRealtime() - backgroundedAt).coerceAtLeast(0).milliseconds
        )
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundedAtMillis = SystemClock.elapsedRealtime()
    }
}
