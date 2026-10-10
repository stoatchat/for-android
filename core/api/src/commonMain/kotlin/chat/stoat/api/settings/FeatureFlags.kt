package chat.stoat.api.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.SpecialUsers

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



object FeatureFlags {
    @FeatureFlag("LabsAccessControl")
    var labsAccessControl by mutableStateOf<LabsAccessControlVariates>(
        LabsAccessControlVariates.Restricted {
            StoatAPI.selfId == SpecialUsers.JENNIFER
        }
    )

    val labsAccessControlGranted: Boolean
        get() = when (labsAccessControl) {
            is LabsAccessControlVariates.Restricted -> (labsAccessControl as LabsAccessControlVariates.Restricted).predicate()
        }

    @FeatureFlag("UserCards")
    var userCards by mutableStateOf<UserCardsVariates>(
        UserCardsVariates.Restricted {
            StoatAPI.selfId?.endsWith("Z") == true
        }
    )

    val userCardsGranted: Boolean
        get() = when (userCards) {
            is UserCardsVariates.Enabled -> true
            is UserCardsVariates.Restricted -> (userCards as UserCardsVariates.Restricted).predicate()
        }

}
