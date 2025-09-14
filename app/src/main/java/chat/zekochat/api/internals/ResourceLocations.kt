package chat.zekochat.api.internals

import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.api
import chat.zekochat.api.schemas.User

object ResourceLocations {
    fun userAvatarUrl(user: User?): String {
        if (user?.avatar != null) {
            return "${PeptideAPI.getCurrentFilesUrl()}/avatars/${user.avatar.id}"
        }
        return "/users/${(user?.id ?: "").ifBlank { "0".repeat(26) }}/default_avatar".api()
    }
}