package chat.stoat.composables.mfa

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import chat.stoat.api.routes.account.MfaResponse
import chat.stoat.api.routes.account.MfaTicket
import chat.stoat.api.routes.account.createMfaTicket
import chat.stoat.api.routes.account.fetchMfaMethods
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Stable
class MfaPromptState {
    var session by mutableStateOf<MfaPromptSession?>(null)
        private set

    private val mutex = Mutex()

    suspend fun request(): MfaTicket? = mutex.withLock {
        val session = MfaPromptSession()
        this.session = session
        try {
            session.loadMethods()
            session.awaitTicket()
        } finally {
            this.session = null
        }
    }
}

/** Only use this when you do not have a view model, otherwise prefer [MfaPromptState] */
@Composable
fun rememberMfaPrompt(): MfaPromptState = remember { MfaPromptState() }

internal sealed interface MfaPromptEvent {
    data class Submit(val response: MfaResponse) : MfaPromptEvent
    data object RetryLoad : MfaPromptEvent
}

@Stable
class MfaPromptSession internal constructor() {
    var methods by mutableStateOf<List<MfaMethod>?>(null)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var selected by mutableStateOf<MfaMethod?>(null)
        private set
    val input = TextFieldState()
    var busy by mutableStateOf(false)
        internal set

    var error by mutableStateOf<String?>(null)
        internal set

    // how many various debounce techniques do we have in the codebase at this point?
    private val events = Channel<MfaPromptEvent>(Channel.RENDEZVOUS)

    fun selectMethod(method: MfaMethod) {
        if (busy || method == selected) return
        selected = method
        input.clearText()
        error = null
    }

    fun submit() {
        val method = selected ?: return
        val text = input.text.toString()
        if (busy || !method.isSubmittable(text)) return
        events.trySend(MfaPromptEvent.Submit(method.buildResponse(text)))
    }

    fun retryLoad() {
        if (!busy) events.trySend(MfaPromptEvent.RetryLoad)
    }

    fun cancel() {
        if (!busy) events.close()
    }

    internal suspend fun awaitTicket(): MfaTicket? {
        while (true) {
            when (val event = events.receiveCatching().getOrNull() ?: return null) {
                MfaPromptEvent.RetryLoad -> loadMethods()

                is MfaPromptEvent.Submit -> {
                    busy = true
                    error = null
                    try {
                        return createMfaTicket(event.response)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = e.message
                    } finally {
                        busy = false
                    }
                }
            }
        }
    }

    internal suspend fun loadMethods() {
        methods = null
        loadError = null
        try {
            val available = fetchMfaMethods()
            val known = available.mapNotNull(MfaMethod::fromApiName).sortedBy { it.ordinal }
            methods = known
            selected = known.firstOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadError = e.message
        }
    }
}
