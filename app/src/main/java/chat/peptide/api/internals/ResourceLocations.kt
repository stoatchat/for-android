package chat.peptide.api.internals

import chat.peptide.api.PeptideAPI
import chat.peptide.api.api
import chat.peptide.api.schemas.User

object ResourceLocations {
    fun userAvatarUrl(user: User?): String {
        if (user?.avatar != null) {
            return "${PeptideAPI.getCurrentFilesUrl()}/avatars/${user.avatar.id}"
        }
        return "/users/${(user?.id ?: "").ifBlank { "0".repeat(26) }}/default_avatar".api()
    }
}