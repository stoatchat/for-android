package chat.stoat.c2dm

import chat.stoat.internals.StoatWebLink
import kotlinx.coroutines.flow.MutableStateFlow

object NotificationDeepLink {
    val pendingNavigation = MutableStateFlow<StoatWebLink?>(null)
}
