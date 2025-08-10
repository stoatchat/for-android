package chat.peptide.sheets

import androidx.compose.runtime.Composable
import chat.peptide.api.RevoltAPI
import chat.peptide.composables.emoji.EmojiPicker

@Composable
fun ReactSheet(messageId: String, onSelect: (String?) -> Unit) {
    val message = RevoltAPI.messageCache[messageId]

    if (message == null) {
        onSelect(null)
        return
    }

    EmojiPicker {
        onSelect(it.removeSurrounding(":"))
    }

}