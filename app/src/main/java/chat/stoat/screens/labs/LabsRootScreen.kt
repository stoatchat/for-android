package chat.stoat.screens.labs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import chat.stoat.api.settings.FeatureFlags
import chat.stoat.navigation.Navigator
import chat.stoat.screens.labs.ui.mockups.NewLoginExperienceMockup
import chat.stoat.screens.labs.ui.sandbox.NewCardSandboxScreen
import chat.stoat.screens.labs.ui.sandbox.SettingsDslSandbox
import chat.stoat.screens.labs.ui.sandbox.TelecomSandbox

annotation class LabsFeature

sealed interface LabsRoute : NavKey {
    data object Home : LabsRoute
    data object NewLoginMockup : LabsRoute
    data object SettingsDslSandbox : LabsRoute
    data object NewCardSandbox : LabsRoute
    data object TelecomSandbox : LabsRoute
}

@Composable
fun LabsGuard(onTurnBack: () -> Unit = {}, content: @Composable () -> Unit) {
    if (!FeatureFlags.labsAccessControlGranted) {
        AlertDialog(
            onDismissRequest = { onTurnBack() },
            confirmButton = {
                TextButton(onClick = { onTurnBack() }) {
                    Text("Turn back")
                }
            },
            title = {
                Text("You don't have access to Labs.")
            },
            text = {
                Text("Labs is where we test new features. However, these features may be unstable and may not work as expected. Hence, access to Labs is restricted.")
            }
        )
    } else {
        content()
    }
}

@Composable
fun LabsRootScreen(topNav: Navigator) {
    val labsBackStack = remember { mutableStateListOf<NavKey>(LabsRoute.Home) }
    val labsNav = remember(labsBackStack) { Navigator(labsBackStack) }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LabsGuard(
            onTurnBack = {
                topNav.popBackStack()
            }
        ) {
            NavDisplay(
                backStack = labsBackStack,
                onBack = { labsNav.popBackStack() },
                entryProvider = entryProvider {
                    entry<LabsRoute.Home> { LabsHomeScreen(labsNav, topNav) }
                    entry<LabsRoute.NewLoginMockup> { NewLoginExperienceMockup(labsNav) }
                    entry<LabsRoute.SettingsDslSandbox> { SettingsDslSandbox(labsNav) }
                    entry<LabsRoute.NewCardSandbox> { NewCardSandboxScreen(labsNav) }
                    entry<LabsRoute.TelecomSandbox> { TelecomSandbox(labsNav) }
                }
            )
        }
    }
}
