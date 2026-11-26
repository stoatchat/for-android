package chat.stoat.platform

import androidx.compose.ui.platform.ClipEntry

/** A clipboard entry holding [text]. [label] is shown by platforms that describe clips. */
expect fun plainTextClipEntry(label: String, text: String): ClipEntry
