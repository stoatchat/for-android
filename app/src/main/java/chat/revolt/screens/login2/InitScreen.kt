package chat.revolt.screens.login2

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chat.revolt.BuildConfig
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.settings.LoadedSettings
import chat.revolt.composables.generic.AnyLink
import chat.revolt.composables.generic.Weblink
import chat.revolt.ui.theme.Theme
import com.chuckerteam.chucker.api.Chucker

@Composable
fun InitScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass
) {
    Box {
        Image(
            painter = painterResource(R.drawable.login_bg),
            modifier = Modifier
                .scale(3f)
                .offset(y = 32.dp)
                .align(Alignment.TopCenter),
            colorFilter = if (LoadedSettings.theme == Theme.M3Dynamic) ColorFilter.tint(
                MaterialTheme.colorScheme.tertiaryContainer
            ) else null,
            contentDescription = null
        )
        if (windowSizeClass.widthSizeClass <= WindowWidthSizeClass.Compact) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.weight(.5f), contentAlignment = Alignment.Center) {
                    LeadPart(windowSizeClass = windowSizeClass)
                }
                Box(Modifier.weight(.5f), contentAlignment = Alignment.Center) {
                    LinkPart(windowSizeClass = windowSizeClass)
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.weight(.5f), contentAlignment = Alignment.Center) {
                    LeadPart(windowSizeClass = windowSizeClass)
                }
                Box(Modifier.weight(.5f), contentAlignment = Alignment.Center) {
                    LinkPart(windowSizeClass = windowSizeClass)
                }
            }
        }
    }
}

@Composable
private fun LeadPart(windowSizeClass: WindowSizeClass) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(horizontal = 16.dp)
    ) {
        if (windowSizeClass.heightSizeClass >= WindowHeightSizeClass.Compact) {
            Spacer(Modifier.height(64.dp))
        }
        Image(
            painter = painterResource(R.drawable.revolt_logo_wide),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                MaterialTheme.colorScheme.onBackground
            ),
            modifier = if (windowSizeClass.widthSizeClass <= WindowWidthSizeClass.Compact)
                Modifier.fillMaxWidth(0.5f) else Modifier.height(32.dp)
        )
        Spacer(modifier = Modifier.height(64.dp))
        Text(
            "Find your community", // FIXME hardcoded string
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Revolt is the chat app that’s truly built with you in mind.", // FIXME hardcoded string
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LinkPart(windowSizeClass: WindowSizeClass) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            8.dp,
            alignment = Alignment.Bottom
        ),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Column(Modifier.widthIn(max = 150.dp)) {
            Button(
                onClick = {/* navController.navigate("login2/existing/details") */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log In") // FIXME hardcoded string
            }
            TextButton(
                onClick = {/* navController.navigate("login2/new/details") */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Up") // FIXME hardcoded string
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Weblink(
            text = stringResource(R.string.terms_of_service),
            url = "${RevoltAPI.getCurrentMarketingUrl()}/terms"
        )
        Weblink(
            text = stringResource(R.string.privacy_policy),
            url = "${RevoltAPI.getCurrentMarketingUrl()}/privacy"
        )
        Weblink(
            text = stringResource(R.string.community_guidelines),
            url = "${RevoltAPI.getCurrentMarketingUrl()}/aup"
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