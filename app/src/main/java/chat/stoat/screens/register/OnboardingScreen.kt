package chat.stoat.screens.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chat.stoat.api.routes.onboard.OnboardingCompletionBody
import chat.stoat.api.routes.onboard.completeOnboarding
import chat.stoat.composables.generic.FormTextField
import chat.stoat.persistence.KVStorage
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.next
import chat.stoat.shared.resources.onboarding_changeable
import chat.stoat.shared.resources.onboarding_lead
import chat.stoat.shared.resources.onboarding_others
import chat.stoat.shared.resources.onboarding_username
import chat.stoat.shared.resources.onboarding_welcome
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingScreen(navController: NavController, onOnboardingComplete: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val username = remember { mutableStateOf("") }
    val error = remember { mutableStateOf("") }

    fun onboardingComplete() {
        onOnboardingComplete()
        navController.popBackStack(
            navController.graph.startDestinationRoute!!,
            inclusive = true
        )
        navController.navigate("default")
    }

    suspend fun onboard() {
        val body = OnboardingCompletionBody(
            username = username.value
        )

        val sessionToken = KVStorage(context).get("sessionToken") ?: return
        val result = completeOnboarding(body, sessionToken)

        if (result.ok) {
            onboardingComplete()
        } else {
            error.value = result.error?.type ?: "Unknown error"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
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
            Text(
                text = stringResource(Res.string.onboarding_welcome),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(Res.string.onboarding_lead),
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.5f
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(Res.string.onboarding_others),
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.5f
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(Res.string.onboarding_changeable),
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.5f
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FormTextField(
                    value = username.value,
                    onChange = { username.value = it },
                    label = stringResource(Res.string.onboarding_username)
                )

                if (error.value.isNotBlank()) {
                    Text(
                        text = error.value,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    onboard()
                }
            },
            enabled = username.value.isNotBlank()
        ) {
            Text(text = stringResource(Res.string.next))
        }
    }
}
