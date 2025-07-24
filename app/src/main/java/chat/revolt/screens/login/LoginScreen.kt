package chat.revolt.screens.login

import android.util.Log
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
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.RevoltApplication
import chat.revolt.api.REVOLT_APP
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.account.EmailPasswordAssessment
import chat.revolt.api.routes.account.negotiateAuthentication
import chat.revolt.api.routes.onboard.needsOnboarding
import chat.revolt.composables.generic.FormTextField
import chat.revolt.composables.generic.Weblink
import chat.revolt.persistence.KVStorage
import chat.revolt.ui.theme.FragmentMono
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val kvStorage: KVStorage
) : ViewModel() {
    private var _email by mutableStateOf("")
    val email: String
        get() = _email

    private var _password by mutableStateOf("")
    val password: String
        get() = _password

    private var _error by mutableStateOf<String?>(null)
    val error: String?
        get() = _error

    private var _navigateTo by mutableStateOf<String?>(null)
    val navigateTo: String?
        get() = _navigateTo

    private var _mfaResponse by mutableStateOf<EmailPasswordAssessment?>(null)
    val mfaResponse: EmailPasswordAssessment?
        get() = _mfaResponse

    fun doLogin() {
        _error = null

        viewModelScope.launch {
            val response = try {
                negotiateAuthentication(_email, _password)
            } catch (e: Exception) {
                _error = if (e.message?.startsWith("Unexpected JSON token") == true) {
                    RevoltApplication.instance.getString(R.string.service_health_alert_body_default)
                } else e.message ?: "Unknown error"
                return@launch
            }
            if (response.error != null) {
                _error = response.error.type
            } else {
                Log.d("Login", "Checking for MFA")
                if (response.proceedMfa) {
                    Log.d("Login", "MFA required. Navigating to MFA screen")
                    _mfaResponse = response
                    _navigateTo = "mfa"
                } else {
                    Log.d(
                        "Login",
                        "No MFA required. Login is complete! We should have a session token"
                    )

                    try {
                        val token = response.firstUserHints!!.token
                        val id = response.firstUserHints.id

                        kvStorage.set("sessionToken", token)
                        kvStorage.set("sessionId", id)

                        val onboard = needsOnboarding(token)
                        if (onboard) {
                            _navigateTo = "onboarding"
                            return@launch
                        }

                        RevoltAPI.loginAs(token)
                        RevoltAPI.setSessionId(response.firstUserHints.token)

                        _navigateTo = "home"
                    } catch (e: Error) {
                        _error = e.message ?: "Unknown error"
                    }
                }
            }
        }
    }

    fun navigationComplete() {
        _navigateTo = null
    }

    fun setEmail(email: String) {
        _email = email
    }

    fun setPassword(password: String) {
        _password = password
    }
}

@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel = hiltViewModel()) {
    val passwordTextFieldState = rememberTextFieldState()
    LaunchedEffect(passwordTextFieldState.text) {
        viewModel.setPassword(passwordTextFieldState.text.toString())
    }
    val showPassword = remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(viewModel.navigateTo) {
        when (viewModel.navigateTo) {
            "mfa" -> {
                navController.navigate(
                    "login/mfa/${viewModel.mfaResponse!!.mfaSpec!!.ticket}/${
                        viewModel.mfaResponse!!.mfaSpec!!.allowedMethods.joinToString(
                            ","
                        )
                    }"
                )
            }

            "home" -> {
                navController.navigate("chat") {
                    popUpTo("login/greeting") { inclusive = true }
                }
            }

            "onboarding" -> {
                navController.navigate("register/onboarding") {
                    popUpTo("login/greeting") { inclusive = true }
                }
            }
        }
        if (viewModel.navigateTo != null) {
            viewModel.navigationComplete()
        }
    }

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
                text = stringResource(R.string.login_heading),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth()
            )

            Column(
                modifier = Modifier
                    .width(270.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FormTextField(
                    value = viewModel.email,
                    label = stringResource(R.string.email),
                    type = KeyboardType.Email,
                    action = ImeAction.Next,
                    onChange = viewModel::setEmail,
                    modifier = Modifier
                        .padding(vertical = 25.dp)
                        .semantics {
                            contentType = ContentType.EmailAddress
                        }
                )
                SecureTextField(
                    passwordTextFieldState,
                    label = { Text(stringResource(R.string.password)) },
                    textObfuscationMode = if (showPassword.value) {
                        TextObfuscationMode.Visible
                    } else {
                        TextObfuscationMode.RevealLastTyped
                    },
                    textStyle = if (showPassword.value) LocalTextStyle.current else LocalTextStyle.current.copy(
                        fontFamily = FragmentMono
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            showPassword.value = !showPassword.value
                        }) {
                            when {
                                showPassword.value -> {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_visibility_off_24dp),
                                        contentDescription = stringResource(R.string.hide_password)
                                    )
                                }

                                else -> {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_visibility_24dp),
                                        contentDescription = stringResource(R.string.show_password)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.semantics {
                        contentType = ContentType.Password
                    }
                )

                Weblink(
                    text = stringResource(R.string.password_forgot),
                    url = "$REVOLT_APP/login/reset",
                    modifier = Modifier.padding(vertical = 7.dp)
                )

                if (viewModel.error != null) {
                    Text(
                        text = viewModel.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp
                        ),
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Weblink(
                text = stringResource(R.string.resend_verification),
                url = "$REVOLT_APP/login/resend",
                modifier = Modifier
                    .padding(vertical = 7.dp)
                    .testTag("resend_verification_link")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row {
                TextButton(onClick = {
                    navController.popBackStack()
                }) {
                    Text(text = stringResource(R.string.back))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(onClick = {
                    viewModel.doLogin()
                }) {
                    Text(text = stringResource(R.string.login))
                }
            }
        }
    }
}
