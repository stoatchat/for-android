package chat.stoat.c2dm

import kotlinx.coroutines.flow.MutableStateFlow

object NotificationDeepLink {
    val pendingChannelId = MutableStateFlow<String?>(null)
}
