package chat.stoat.core.model.schemas

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class User(
    @SerialName("_id")
    val id: String? = null,
    val username: String? = null,
    val discriminator: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    val pronouns: String? = null,
    val avatar: AutumnResource? = null,
    val relations: List<Relation>? = null,
    val badges: Long? = null,
    val status: Status? = null,
    val profile: Profile? = null,
    val flags: Long? = null,
    val privileged: Boolean? = null,
    val bot: Bot? = null,
    val relationship: String? = null,
    val online: Boolean? = null
) : Parcelable {
    fun mergeWithPartial(partial: User): User {
        return User(
            id = partial.id ?: id,
            username = partial.username ?: username,
            discriminator = partial.discriminator ?: discriminator,
            displayName = partial.displayName ?: displayName,
            pronouns = partial.pronouns ?: pronouns,
            avatar = partial.avatar ?: avatar,
            relations = partial.relations ?: relations,
            badges = partial.badges ?: badges,
            status = partial.status ?: status,
            profile = partial.profile ?: profile,
            flags = partial.flags ?: flags,
            privileged = partial.privileged ?: privileged,
            bot = partial.bot ?: bot,
            relationship = partial.relationship ?: relationship,
            online = partial.online ?: online
        )
    }

    companion object {
        fun getPlaceholder(forId: String) = User(
            id = forId,
            username = "Unknown User",
            discriminator = "0000",
            displayName = null,
            pronouns = null,
            avatar = null,
            badges = 0,
            status = null,
            profile = null,
            flags = 0,
            privileged = false,
            bot = null,
            relationship = null,
            online = false
        )

        fun resolveDefaultName(user: User, withDiscriminator: Boolean = false): String {
            val maybeDiscriminator = if (withDiscriminator) "#${user.discriminator}" else ""
            return user.displayName ?: "${user.username}$maybeDiscriminator"
        }
    }
}

enum class UserBadges(val value: Long) {
    Developer(1L shl 0),
    Translator(1L shl 1),
    Supporter(1L shl 2),
    ResponsibleDisclosure(1L shl 3),
    Founder(1L shl 4),
    PlatformModeration(1L shl 5),
    ActiveSupporter(1L shl 6),
    Paw(1L shl 7),
    EarlyAdopter(1L shl 8),
    ReservedRelevantJokeBadge1(1L shl 9),
    ReservedRelevantJokeBadge2(1L shl 10),
}

infix fun Long?.has(flag: UserBadges): Boolean {
    if (this == null) return false
    return this and flag.value == flag.value
}

@Serializable
@Parcelize
data class Bot(
    val owner: String? = null
) : Parcelable

@Serializable
@Parcelize
data class Profile(
    val content: String? = null,
    val background: AutumnResource? = null
) : Parcelable

@Serializable
@Parcelize
data class Relation(
    @SerialName("_id")
    val id: String? = null,

    val status: String? = null
) : Parcelable

@Serializable
@Parcelize
data class Status(
    val text: String? = null,
    val presence: String? = null
) : Parcelable
