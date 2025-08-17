package chat.peptide.screens.login

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import chat.peptide.R
import chat.peptide.composables.generic.SquareButton
import chat.peptide.composables.generic.SquareElevatedButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginGreetingScreen(navController: NavController) {
    val context = LocalContext.current
    var catTaps by remember { mutableIntStateOf(0) }
    var showBoringButton by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 20.dp, horizontal = 16.dp)
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.onboarding_fly_image),
                contentDescription = "Peptide Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .combinedClickable(
                        interactionSource = remember(::MutableInteractionSource),
                        indication = null,
                        onClick = {
                            if (catTaps < 9) {
                                catTaps++
                            } else {
                                Toast
                                    .makeText(
                                        context,
                                        "🐈",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                                catTaps = 0
                            }
                        },
                        onLongClick = {
                            showBoringButton = !showBoringButton
                        }
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.login_onboarding_heading),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.login_onboarding_body),
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.5f
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            SquareButton(
                onClick = { navController.navigate("login/login") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_login_page_button"),
            ) {
                Text(text = stringResource(R.string.login))
            }

            Spacer(modifier = Modifier.height(5.dp))

            SquareElevatedButton(
                onClick = { navController.navigate("register/details") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_signup_page_button"),
            ) {
                Text(text = stringResource(R.string.signup))
            }

            AnimatedVisibility(showBoringButton) {
                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = { navController.navigate("login2/init") },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Try new login experience", textAlign = TextAlign.Center)
                        Text(
                            text = "(beta)",
                            color = LocalContentColor.current.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginGreetingScreenPreview() {
    val navController = rememberNavController()
    LoginGreetingScreen(navController)
}

