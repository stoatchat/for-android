package chat.stoat.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.routes.account.MfaSettings
import chat.stoat.api.routes.account.fetchMfaSettings
import chat.stoat.settings.dsl.SettingsPage

class MfaSettingsScreenViewModel() : ViewModel() {
    var mfaStateLoaded by mutableStateOf(false)
        private set
    var mfaState by mutableStateOf<MfaSettings?>(null)
        private set

    suspend fun loadDetails() {
        runCatching { fetchMfaSettings() }
            .onSuccess { mfaState = it }
            .also { mfaStateLoaded = true }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MfaSettingsScreen(
    navController: NavController,
    viewModel: MfaSettingsScreenViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadDetails()
    }

    SettingsPage(
        navController,
        title = { Text(stringResource(R.string.settings_mfa)) }
    ) {
        AnimatedContent(
            targetState = viewModel.mfaStateLoaded,
        ) { loaded ->
            if (!loaded) {
                Box(
                    Modifier
                        .height(200.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(
                        modifier = Modifier.size(72.dp)
                    )
                }
            } else {
                Text("MFA Settings: ${viewModel.mfaState}")
            }
        }
    }
}