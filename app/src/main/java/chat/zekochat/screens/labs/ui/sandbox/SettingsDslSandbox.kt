package chat.zekochat.screens.labs.ui.sandbox

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import chat.zekochat.settings.dsl.SettingsPage
import chat.zekochat.settings.dsl.SubcategoryContentInsets

enum class SettingsDslSandboxTab {
    General,
    Appearance,
}

@Composable
fun SettingsDslSandbox(navController: NavController) {
    SettingsPage(
        navController = navController,
        title = {
            Text(
                text = "Settings DSL Demo",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    ) {
        Subcategory(
            title = { Text("General") },
            contentInsets = SubcategoryContentInsets
        ) {
            Text(text = "General settings")
        }

        Subcategory(
            title = { Text("Appearance") }
        ) {
            RadioOptions(
                options = SettingsDslSandboxTab.entries,
                selectedOption = SettingsDslSandboxTab.General,
                onOptionSelected = { }
            )
        }
    }
}