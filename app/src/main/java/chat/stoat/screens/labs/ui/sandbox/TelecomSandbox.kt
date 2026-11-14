package chat.stoat.screens.labs.ui.sandbox

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import chat.stoat.activities.voice.IncomingActivity
import chat.stoat.navigation.Navigator
import chat.stoat.settings.dsl.SettingsPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

@Composable
fun TelecomSandbox(navigator: Navigator) {
    val context = LocalContext.current
    val callsManager = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) CallsManager(context) else null
    }
    val didRegister = remember { mutableStateOf(false) }
    val callControlScope = remember { mutableStateOf<CallControlScope?>(null) }
    val scope = rememberCoroutineScope()

    SettingsPage(
        navigator,
        title = {
            Text(
                text = "Telecom Sandbox",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    ) {
        Column {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val capabilities: @CallsManager.Companion.Capability Int =
                            (CallsManager.CAPABILITY_BASELINE or
                                    CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING)

                        callsManager?.registerAppWithTelecom(capabilities)
                        didRegister.value = true
                    } else {
                        showSnackbar("Android version too old for Core-Telecom API")
                    }
                }
            ) {
                Text(if (didRegister.value) "Registered" else "Register for Calls")
            }

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val address = "stoat:01F92C5ZXBQWQ8KY7J8KY917NM".toUri()
                        val attr = CallAttributesCompat(
                            displayName = "Jennifer",
                            address = address,
                            direction = CallAttributesCompat.DIRECTION_INCOMING,
                            callType = CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                            callCapabilities = 0
                        )
                        scope.launch {
                            delay(5000)
                            try {
                                callsManager?.addCall(
                                    attr,
                                    { someInt ->
                                        logcat { "Call answered with $someInt" }
                                        // answer the call here, e.g. start an activity or service...
                                    }, // Watch needs to know if it can answer the call.
                                    { cause ->
                                        logcat { "Call ended with cause $cause" }
                                    },
                                    {
                                        logcat { "Call state changed to active" }
                                    },
                                    {
                                        logcat { "Call state changed to inactive (hold)" }
                                    }
                                ) {
                                    // The call was successfully added once this scope runs.
                                    callControlScope.value = this
                                }
                            } catch (addCallException: Exception) {
                                logcat(LogPriority.ERROR) { addCallException.asLog() }
                            }
                        }
                    } else {
                        showSnackbar("Android version too old for Core-Telecom API")
                    }
                }
            ) {
                Text("Place incoming call in 5 seconds")
            }

            Button(
                onClick = {
                    val intent = Intent(context, IncomingActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            ) {
                Text("Just open incoming call activity")
            }
        }
    }
}