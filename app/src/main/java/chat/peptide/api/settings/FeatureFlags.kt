package chat.peptide.api.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.peptide.api.RevoltAPI
import chat.peptide.api.internals.SpecialUsers

annotation class FeatureFlag(val name: String)
annotation class Treatment(val description: String)

@FeatureFlag("LabsAccessControl")
sealed class LabsAccessControlVariates {
    @Treatment(
        "Restrict access to Labs to users that meet certain or all criteria (implementation-specific)"
    )
    data class Restricted(val predicate: () -> Boolean) : LabsAccessControlVariates()
}

@FeatureFlag("UserCards")
sealed class UserCardsVariates {
    @Treatment(
        "Enable user cards for all users"
    )
    object Enabled : UserCardsVariates()

    @Treatment(
        "Enable user cards for users that meet certain or all criteria (implementation-specific)"
    )
    data class Restricted(val predicate: () -> Boolean) : UserCardsVariates()
}

@FeatureFlag("MassMentions")
sealed class MassMentionsVariates {
    @Treatment(
        "Enable mass mentions and role mentions for all users"
    )
    object Enabled : MassMentionsVariates()

    @Treatment(
        "Disable mass mentions and role mentions for all users"
    )
    object Disabled : MassMentionsVariates()
}

@FeatureFlag("VoiceChannels2_0")
sealed class VoiceChannels2_0Variates {
    @Treatment(
        "Enable the new voice channels 2.0 for all users"
    )
    object Enabled : VoiceChannels2_0Variates()

    @Treatment(
        "Disable the new voice channels 2.0 for all users"
    )
    object Disabled : VoiceChannels2_0Variates()
}

object FeatureFlags {
    @FeatureFlag("LabsAccessControl")
    var labsAccessControl by mutableStateOf<LabsAccessControlVariates>(
        LabsAccessControlVariates.Restricted {
            RevoltAPI.selfId == SpecialUsers.JENNIFER
        }
    )

    val labsAccessControlGranted: Boolean
        get() = when (labsAccessControl) {
            is LabsAccessControlVariates.Restricted -> (labsAccessControl as LabsAccessControlVariates.Restricted).predicate()
        }

    @FeatureFlag("UserCards")
    var userCards by mutableStateOf<UserCardsVariates>(
        UserCardsVariates.Restricted {
            RevoltAPI.selfId?.endsWith("Z") == true
        }
    )

    val userCardsGranted: Boolean
        get() = when (userCards) {
            is UserCardsVariates.Enabled -> true
            is UserCardsVariates.Restricted -> (userCards as UserCardsVariates.Restricted).predicate()
        }

    @FeatureFlag("MassMentions")
    var massMentions by mutableStateOf<MassMentionsVariates>(
        MassMentionsVariates.Disabled
    )
    val massMentionsGranted: Boolean
        get() = when (massMentions) {
            is MassMentionsVariates.Enabled -> true
            is MassMentionsVariates.Disabled -> false
        }

    @FeatureFlag("VoiceChannels2_0")
    var voiceChannels2_0 by mutableStateOf<VoiceChannels2_0Variates>(
        VoiceChannels2_0Variates.Disabled
    )
    val voiceChannels2_0Granted: Boolean
        get() = when (voiceChannels2_0) {
            is VoiceChannels2_0Variates.Enabled -> true
            is VoiceChannels2_0Variates.Disabled -> false
        }
}
