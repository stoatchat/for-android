package chat.stoat.screens.labs.ui.mockups

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.core.model.settings.Theme
import chat.stoat.navigation.Navigator
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.login_bg
import chat.stoat.shared.resources.stoat_wordmark_white
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewLoginExperienceMockup(navigator: Navigator) {
    val loginBackStack = remember { mutableStateListOf(MockupStep.Greet) }

    NavDisplay(
        backStack = loginBackStack,
        transitionSpec = {
            slideIn(tween(300)) { IntOffset(it.width, 0) } togetherWith
                slideOut(tween(300)) { IntOffset(-it.width, 0) }
        },
        popTransitionSpec = {
            slideIn(tween(300)) { IntOffset(-it.width, 0) } togetherWith
                slideOut(tween(300)) { IntOffset(it.width, 0) }
        },
        entryProvider = entryProvider {
            entry(MockupStep.Greet) {
                Box {
                    Image(
                        painter = painterResource(Res.drawable.login_bg),
                        modifier = Modifier
                            .scale(3f)
                            .offset(y = 32.dp)
                            .align(Alignment.TopCenter),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiaryContainer),
                        contentDescription = null
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(0.5f)
                                .padding(horizontal = 16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(64.dp))
                            Image(
                                painter = painterResource(Res.drawable.stoat_wordmark_white),
                                contentDescription = null,
                                colorFilter = if (LoadedSettings.theme == Theme.M3Dynamic) ColorFilter.tint(
                                    MaterialTheme.colorScheme.onBackground
                                ) else null,
                                modifier = Modifier.fillMaxWidth(0.5f)
                            )
                            Spacer(modifier = Modifier.height(64.dp))
                            Text(
                                "Find your community",
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Stoat is the chat app that’s truly built with you in mind.",
                                style = MaterialTheme.typography.bodyLargeEmphasized,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(
                                8.dp,
                                alignment = Alignment.Bottom
                            ),
                            modifier = Modifier
                                .weight(0.5f)
                                .padding(horizontal = 16.dp)
                                .widthIn(max = 150.dp)
                        ) {
                            Button(
                                onClick = { loginBackStack.add(MockupStep.Login) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Log In")
                            }
                            TextButton(
                                onClick = { navigator.popBackStack() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sign Up")
                            }
                            Spacer(modifier = Modifier.fillMaxHeight(0.25f))
                        }
                    }
                }
            }
            entry(MockupStep.Login) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Blue), Alignment.Center
                ) {
                    Button(onClick = { loginBackStack.add(MockupStep.Greet) }) {
                        Text("Greetings!")
                    }
                }
            }
        }
    )
}

private enum class MockupStep { Greet, Login }
