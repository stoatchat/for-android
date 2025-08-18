package chat.peptide.screens.labs.ui.mockups

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import chat.peptide.R
import chat.peptide.api.settings.LoadedSettings
import chat.peptide.composables.generic.PepTextButton
import chat.peptide.composables.generic.SquareButton
import chat.peptide.ui.theme.Theme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewLoginExperienceMockup(navController: NavController) {
    val loginNav = rememberNavController()

    NavHost(
        navController = loginNav,
        startDestination = "greet",
        enterTransition = {
            slideIn(
                tween(300)
            ) { IntOffset(it.width, 0) }
        },
        exitTransition = {
            slideOut(
                tween(300)
            ) { IntOffset(-it.width, 0) }
        },
        popEnterTransition = {
            slideIn(
                tween(300)
            ) { IntOffset(-it.width, 0) }
        },
        popExitTransition = {
            slideOut(
                tween(300)
            ) { IntOffset(it.width, 0) }
        }
    ) {
        composable("greet") {
            Box {
                Image(
                    painter = painterResource(R.drawable.login_bg),
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
                            painter = painterResource(R.drawable.peptide_logo_wide),
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
                            "Peptide is the chat app that’s truly built with you in mind.",
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
                        SquareButton(
                            onClick = { loginNav.navigate("login") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Log In")
                        }
                        PepTextButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign Up")
                        }
                        Spacer(modifier = Modifier.fillMaxHeight(0.25f))
                    }
                }
            }
        }
        composable("login") {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Blue), Alignment.Center
            ) {
                SquareButton(onClick = { loginNav.navigate("greet") }) {
                    Text("Greetings!")
                }
            }
        }
    }
}
