package chat.stoat.api.internals

import android.os.Build
import android.os.SystemClock
import chat.stoat.api.StoatAPIHost
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

internal actual fun platformUserAgent(): String =
    "StoatForAndroid/${StoatAPIHost.config.versionName} ${StoatAPIHost.config.applicationId} " +
        "Android/${Build.VERSION.SDK_INT} (${Build.MANUFACTURER} ${Build.DEVICE})"

actual fun friendlySessionName(): String = "Stoat for Android on ${Build.MANUFACTURER} ${Build.MODEL}"

internal actual fun defaultHttpEngine(): HttpClientEngine = OkHttp.create()
