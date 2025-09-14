package chat.zekochat.screens.labs.ui.sandbox

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import chat.zekochat.composables.generic.SquareButton
import chat.zekochat.settings.dsl.SettingsPage
import chat.zekochat.ui.theme.FragmentMono

@Composable
fun CoreLibSandbox(navController: NavController) {
    var greeting by remember { mutableStateOf("<no greeting>") }

    SettingsPage(
        navController,
        title = {
            Text(
                text = buildAnnotatedString {
                    pushStyle(SpanStyle(fontFamily = FragmentMono))
                    append("libpeptide")
                    pop()
                    append(" Sample")
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    ) {
        SquareButton(onClick = {
            greeting = librevolt.greet()
        }) {
            Text("Greet")
        }
        Text(greeting)
    }
}