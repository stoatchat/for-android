package chat.stoat.screens.login2

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
import chat.stoat.BuildConfig
import chat.stoat.R
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.composables.generic.AnyLink
import chat.stoat.composables.generic.Weblink
import chat.stoat.core.model.data.STOAT_MARKETING
import chat.stoat.ui.theme.Theme
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
            painter = painterResource(R.drawable.stoat_logo_white),
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
            "Stoat is the chat app that’s truly built with you in mind.", // FIXME hardcoded string
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