package chat.stoat.screens.register

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.stoat.api.routes.account.RegistrationBody
import chat.stoat.api.routes.account.register
import chat.stoat.api.routes.misc.getRootRoute
import chat.stoat.composables.generic.FormTextField
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.back
import chat.stoat.shared.resources.register_data
import chat.stoat.shared.resources.register_email
import chat.stoat.shared.resources.register_email_verification_hint
import chat.stoat.shared.resources.register_form_heading
import chat.stoat.shared.resources.register_password
import chat.stoat.shared.resources.register_password_rules
import chat.stoat.shared.resources.service_health_alert_body_default
import chat.stoat.shared.resources.signup
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaSize
import com.hcaptcha.sdk.HCaptchaTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

class RegisterDetailsScreenViewModel : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    private var captchaToken by mutableStateOf<String?>(null)

    fun initCaptcha(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val root = try {
                getRootRoute()
            } catch (e: Exception) {
                error = if (e.message?.startsWith("Expected response body of the type") == true) {
                    getString(Res.string.service_health_alert_body_default)
                } else e.message
                return@launch
            }

            if (!root.features.captcha.enabled) {
                onSuccess()
                return@launch
            }

            val config = HCaptchaConfig.builder().apply {
                siteKey(root.features.captcha.key)
                theme(HCaptchaTheme.DARK)
                size(HCaptchaSize.INVISIBLE)
            }.build()

            HCaptcha.getClient(context).apply {
                addOnSuccessListener {
                    captchaToken = it.tokenResult
                    onSuccess()
                }

                addOnFailureListener {
                    error = it.message
                }

                setup(config)
                verifyWithHCaptcha()
            }
        }
    }

    fun doRegistration(navController: NavController) {
        val body = RegistrationBody(
            email = email,
            password = password,
            captcha = captchaToken ?: ""
        )

        viewModelScope.launch {
            val result = register(body)

            if (result.ok) {
                navController.navigate("register/verify/$email")
            } else {
                error = result.unwrapError().type
            }
        }
    }
}

@Composable
fun RegisterDetailsScreen(
    navController: NavController,
    viewModel: RegisterDetailsScreenViewModel = viewModel()
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .imePadding()
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
                text = stringResource(Res.string.register_form_heading),
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
                text = stringResource(Res.string.register_data),
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
                    value = viewModel.email,
                    onChange = { viewModel.email = it },
                    label = stringResource(Res.string.register_email),
                    type = KeyboardType.Email,
                    action = ImeAction.Next,
                    modifier = Modifier.semantics { contentType = ContentType.EmailAddress }
                )
                Text(
                    text = stringResource(Res.string.register_email_verification_hint),
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.5f
                    ),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                FormTextField(
                    value = viewModel.password,
                    onChange = { viewModel.password = it },
                    label = stringResource(Res.string.register_password),
                    type = KeyboardType.Password,
                    action = ImeAction.Done,
                    modifier = Modifier.semantics { contentType = ContentType.NewPassword }
                )
                Text(
                    text = stringResource(Res.string.register_password_rules),
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.5f
                    ),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 10.dp)
                )

                if (!viewModel.error.isNullOrBlank()) {
                    Text(
                        text = viewModel.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Row {
            TextButton(onClick = {
                navController.popBackStack()
            }) {
                Text(text = stringResource(Res.string.back))
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = {
                    viewModel.initCaptcha(context) {
                        viewModel.doRegistration(navController)
                    }
                },
                enabled = viewModel.email.isNotBlank() && viewModel.password.isNotBlank()
            ) {
                Text(text = stringResource(Res.string.signup))
            }
        }
    }
}
