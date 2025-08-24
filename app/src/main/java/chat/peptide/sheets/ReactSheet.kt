package chat.peptide.sheets

import androidx.compose.runtime.Composable
import chat.peptide.api.PeptideAPI
import chat.peptide.composables.emoji.EmojiPicker

@Composable
fun ReactSheet(messageId: String, onSelect: (String?) -> Unit) {
    val message = PeptideAPI.messageCache[messageId]

    if (message == null) {
        onSelect(null)
        return
    }

    EmojiPicker {
        onSelect(it.removeSurrounding(":"))
    }

}