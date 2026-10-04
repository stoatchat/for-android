package chat.stoat.internals.extensions

import android.net.Uri
import androidx.core.net.toUri
import chat.stoat.core.model.data.STOAT_INVITES
import chat.stoat.core.model.data.STOAT_WEB_APP

fun Uri.isInviteUri(): Boolean {
    val firstPathSegmentIsInvite = this.pathSegments.firstOrNull() == "invite"
    val isStoatChat = this.host == STOAT_WEB_APP.toUri().host
    val matchSttGG = this.host == STOAT_INVITES.toUri().host

    val matchApp = isStoatChat && firstPathSegmentIsInvite

    val hasEnoughSegments =
        if (matchApp) this.pathSegments.size == 2 else this.pathSegments.size == 1

    return (matchApp || matchSttGG) && hasEnoughSegments
}
