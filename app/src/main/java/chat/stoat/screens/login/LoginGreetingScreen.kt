package chat.stoat.screens.login

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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chat.stoat.BuildConfig
import chat.stoat.R
import chat.stoat.composables.generic.AnyLink
import chat.stoat.composables.generic.Weblink
import chat.stoat.core.model.data.STOAT_MARKETING
import com.chuckerteam.chucker.api.Chucker

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginGreetingScreen(navController: NavController) {
    val context = LocalContext.current
    var catTaps by remember { mutableIntStateOf(0) }
    var showBoringButton by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 20.dp, horizontal = 0.dp)
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
                painter = painterResource(id = R.drawable.stoat_logo_white),
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                contentDescription = "Stoat",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(100.dp)
                    .padding(bottom = 15.dp)
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
                            if (BuildConfig.DEBUG) showBoringButton = !showBoringButton
                        }
                    )
            )

            Text(
                text = stringResource(R.string.login_onboarding_heading),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
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
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth()
            )
        }

        Column(
            modifier = Modifier
                .width(200.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { navController.navigate("login/login") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_login_page_button")
            ) {
                Text(text = stringResource(R.string.login))
            }

            Spacer(modifier = Modifier.height(5.dp))

            ElevatedButton(
                onClick = { navController.navigate("register/greeting") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_signup_page_button")
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

            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(textAlign = TextAlign.Center)
            ) {
                Weblink(
                    text = stringResource(R.string.terms_of_service),
                    url = "$STOAT_MARKETING/terms"
                )
                Weblink(
                    text = stringResource(R.string.privacy_policy),
                    url = "$STOAT_MARKETING/privacy"
                )
                Weblink(
                    text = stringResource(R.string.community_guidelines),
                    url = "$STOAT_MARKETING/aup"
                )
                if (BuildConfig.DEBUG) {
                    AnyLink(
                        text = "Debug: Chucker",
                        action = {
                            Chucker.getLaunchIntent(context).apply {
                                context.startActivity(this)
                            }
                        }
                    )
                }
            }
        }
    }
}
