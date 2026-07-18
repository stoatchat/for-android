package chat.stoat.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telecom.DisconnectCause
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import chat.stoat.R
import chat.stoat.activities.MainActivity
import chat.stoat.c2dm.ChannelRegistrator
import chat.stoat.services.OngoingCallService.Companion.hangupEvents
import chat.stoat.voice.VoiceCallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import kotlin.time.Duration.Companion.seconds

/**
 * Foreground service that runs while the user is connected to a voice channel.
 *
 * Posts a persistent [NotificationCompat.CallStyle] notification with a chronometer (the fancy chip!)
 * and registers the call with the Telecom framework via [CallsManager] so the system treats it like
 * any other ongoing call.
 *
 * The service does not own the call itself, the LiveKit room lives in the voice UIs composition.
 * Hangup requests from the notification or Telecom are relayed through [hangupEvents].
 */
class OngoingCallService : Service() {
    companion object {
        private const val ACTION_START = "chat.stoat.services.OngoingCallService.START"
        private const val ACTION_HANGUP = "chat.stoat.services.OngoingCallService.HANGUP"
        private const val ACTION_STOP = "chat.stoat.services.OngoingCallService.STOP"
        private const val ACTION_TOGGLE_MUTE = "chat.stoat.services.OngoingCallService.TOGGLE_MUTE"
        private const val ACTION_UPDATE_STATE =
            "chat.stoat.services.OngoingCallService.UPDATE_STATE"

        private const val EXTRA_CHANNEL_ID = "channelId"
        private const val EXTRA_CHANNEL_NAME = "channelName"
        private const val EXTRA_MIC_MUTED = "micMuted"

        private const val NOTIFICATION_ID = 67

        /**
         * Emits when the user hangs up from outside the app (notification action or Telecom,
         * e.g. through a smartwatch).
         */
        val hangupEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        fun start(context: Context, channelId: String, channelName: String?) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, OngoingCallService::class.java)
                    .setAction(ACTION_START)
                    .putExtra(EXTRA_CHANNEL_ID, channelId)
                    .putExtra(EXTRA_CHANNEL_NAME, channelName)
            )
        }

        fun updateMicMuted(context: Context, muted: Boolean) {
            try {
                context.startService(
                    Intent(context, OngoingCallService::class.java)
                        .setAction(ACTION_UPDATE_STATE)
                        .putExtra(EXTRA_MIC_MUTED, muted)
                )
            } catch (e: IllegalStateException) {
                logcat(LogPriority.WARN) {
                    "Could not deliver mic state to call service\n" + e.asLog()
                }
            }
        }

        fun stop(context: Context) {
            try {
                context.startService(
                    Intent(context, OngoingCallService::class.java).setAction(ACTION_STOP)
                )
            } catch (e: IllegalStateException) {
                logcat(LogPriority.WARN) { "Could not deliver stop to call service\n" + e.asLog() }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var telecomJob: Job? = null
    private var callControl: CallControlScope? = null
    private var registeredChannelId: String? = null
    private var currentChannelName: String? = null
    private var callStartedAt = 0L
    private var isMicMuted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: return START_NOT_STICKY
                val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME)
                    ?: getString(R.string.voice_notification_fallback_channel_name)

                currentChannelName = channelName
                if (registeredChannelId != channelId) {
                    callStartedAt = System.currentTimeMillis()
                }

                ChannelRegistrator(this).register()
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    buildNotification(channelName),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    } else {
                        0
                    }
                )

                if (registeredChannelId != channelId) {
                    registeredChannelId = channelId
                    registerCallWithTelecom(channelId, channelName)
                }
            }

            ACTION_HANGUP -> hangupEvents.tryEmit(Unit)

            ACTION_TOGGLE_MUTE -> VoiceCallManager.toggleMicrophone()

            ACTION_UPDATE_STATE -> {
                val muted = intent.getBooleanExtra(EXTRA_MIC_MUTED, false)
                if (muted != isMicMuted) {
                    isMicMuted = muted
                    currentChannelName?.let { channelName ->
                        try {
                            NotificationManagerCompat.from(this)
                                .notify(NOTIFICATION_ID, buildNotification(channelName))
                        } catch (e: SecurityException) {
                            logcat(LogPriority.WARN) {
                                "Could not update call notification\n" + e.asLog()
                            }
                        }
                    }
                }
            }

            ACTION_STOP -> endCall(startId)
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        hangupEvents.tryEmit(Unit)
        endCall(startId = -1)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(channelName: String): Notification {
        val caller = Person.Builder()
            .setName(channelName)
            .setImportant(true)
            .build()

        val hangupIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, OngoingCallService::class.java).setAction(ACTION_HANGUP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val toggleMuteIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, OngoingCallService::class.java).setAction(ACTION_TOGGLE_MUTE),
            PendingIntent.FLAG_IMMUTABLE
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ChannelRegistrator.CHANNEL_ID_GROUP_VOICE_ONGOING)
            .setSmallIcon(R.drawable.ic_notification_monochrome)
            .setContentTitle(channelName)
            .setContentText(getString(R.string.voice_notification_ongoing_call))
            .setStyle(NotificationCompat.CallStyle.forOngoingCall(caller, hangupIntent))
            .addAction(
                if (isMicMuted) R.drawable.ic_mic_24dp else R.drawable.ic_mic_off_24dp,
                getString(
                    if (isMicMuted) R.string.voice_action_unmute else R.string.voice_action_mute
                ),
                toggleMuteIntent
            )
            .addPerson(caller)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(true)
            .setWhen(callStartedAt)
            .setUsesChronometer(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .apply {
                if (Build.VERSION.SDK_INT >= 36) {
                    // TODO update sdk and dont use the literal string value here (lazy solution prevailed)
                    extras.putBoolean("android.requestPromotedOngoing", true)
                }
            }
            .build()
    }

    private fun registerCallWithTelecom(channelId: String, channelName: String) {
        telecomJob?.cancel()
        callControl = null

        val callsManager = CallsManager(this)
        try {
            callsManager.registerAppWithTelecom(CallsManager.CAPABILITY_BASELINE)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "Could not register app with Telecom\n" + e.asLog() }
            return
        }

        telecomJob = scope.launch {
            try {
                callsManager.addCall(
                    CallAttributesCompat(
                        displayName = channelName,
                        address = "stoat:$channelId".toUri(),
                        direction = CallAttributesCompat.DIRECTION_OUTGOING,
                        callType = CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                        callCapabilities = 0
                    ),
                    onAnswer = {},
                    onDisconnect = {
                        if (callControl != null) hangupEvents.tryEmit(Unit)
                    },
                    onSetActive = {},
                    onSetInactive = {}
                ) {
                    callControl = this
                    launch { setActive() }
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "Could not add call to Telecom\n" + e.asLog() }
            }
        }
    }

    private fun endCall(startId: Int) {
        val control = callControl
        callControl = null
        registeredChannelId = null

        if (control == null) {
            telecomJob?.cancel()
            stopSelf(startId)
            return
        }

        scope.launch {
            try {
                withTimeoutOrNull(1.seconds) {
                    control.disconnect(DisconnectCause(DisconnectCause.LOCAL))
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "Could not disconnect Telecom call\n" + e.asLog() }
            }
            stopSelf(startId)
        }
    }
}
