package chat.peptide.callbacks

import kotlinx.coroutines.flow.MutableSharedFlow

sealed class UiCallback {
    data class ReplyToMessage(val messageId: String) : UiCallback()
    data class ReplyToMessageWithContent(val messageId: String, val content: String) : UiCallback()
    data class EditMessage(val messageId: String) : UiCallback()
}

object UiCallbacks {
    val uiCallbackFlow: MutableSharedFlow<UiCallback> = MutableSharedFlow()

    suspend fun replyToMessage(messageId: String) {
        uiCallbackFlow.emit(UiCallback.ReplyToMessage(messageId))
    }

    suspend fun replyToMessageWithContent(messageId: String, content: String) {
        uiCallbackFlow.emit(UiCallback.ReplyToMessageWithContent(messageId, content))
    }

    suspend fun editMessage(messageId: String) {
        uiCallbackFlow.emit(UiCallback.EditMessage(messageId))
    }
}
