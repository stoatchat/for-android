package chat.stoat.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface Route : NavKey {
    @Serializable data object Default : Route

    @Serializable
    sealed interface Login : Route {
        @Serializable data object Greeting : Login
        @Serializable data object Credentials : Login
        @Serializable data class Mfa(val ticket: String, val allowedAuthTypes: List<String>) : Login
    }

    @Serializable
    sealed interface Register : Route {
        @Serializable data object Greeting : Register
        @Serializable data object Details : Register
        @Serializable data class Verify(val email: String) : Register
        @Serializable data object Onboarding : Register
    }

    @Serializable
    sealed interface Login2 : Route {
        @Serializable data object Init : Login2
    }

    @Serializable data object Chat : Route

    @Serializable
    sealed interface Main : Route {
        @Serializable data object Home : Main
        @Serializable data class Conversation(val channelId: String) : Main
    }

    @Serializable data object CatchUp : Route
    @Serializable data object CreateGroup : Route
    @Serializable data object Discover : Route

    @Serializable
    sealed interface Settings : Route {
        @Serializable data object Home : Settings
        @Serializable data object Account : Settings
        @Serializable data object AccountMfa : Settings
        @Serializable data object Profile : Settings
        @Serializable data object Sessions : Settings
        @Serializable data object Appearance : Settings
        @Serializable data object Chat : Settings
        @Serializable data object Notifications : Settings
        @Serializable data object Debug : Settings
        @Serializable data object Experiments : Settings
        @Serializable data object Language : Settings
    }

    @Serializable
    sealed interface ChannelSettings : Route {
        val channelId: String

        @Serializable data class Home(override val channelId: String) : ChannelSettings
        @Serializable data class Overview(override val channelId: String) : ChannelSettings
        @Serializable data class Permissions(override val channelId: String) : ChannelSettings
        @Serializable
        data class PermissionEditor(override val channelId: String, val roleId: String) : ChannelSettings
    }

    @Serializable
    sealed interface ServerSettings : Route {
        val serverId: String

        @Serializable data class Home(override val serverId: String) : ServerSettings
        @Serializable
        data class Identity(override val serverId: String, val userId: String? = null) : ServerSettings
        @Serializable data class Overview(override val serverId: String) : ServerSettings
        @Serializable data class Emojis(override val serverId: String) : ServerSettings
        @Serializable data class Invites(override val serverId: String) : ServerSettings
        @Serializable data class Bans(override val serverId: String) : ServerSettings
        @Serializable data class Channels(override val serverId: String) : ServerSettings
        @Serializable data class Roles(override val serverId: String) : ServerSettings
        @Serializable data class RoleEditor(override val serverId: String, val roleId: String) : ServerSettings
    }

    @Serializable
    sealed interface Channel : Route {
        val channelId: String

        @Serializable data class Pins(override val channelId: String) : Channel
        @Serializable data class Search(override val channelId: String) : Channel
    }

    @Serializable
    sealed interface About : Route {
        @Serializable data object Home : About
        @Serializable data object Oss : About
    }

    @Serializable data object Labs : Route

    @Serializable data class Changelog(val id: String) : Route
}

@OptIn(ExperimentalSerializationApi::class)
val RouteSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed(Route.serializer())
        }
    }
}
