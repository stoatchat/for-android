package chat.zekochat.screens.login

import android.util.Log
import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
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
import chat.zekochat.R
import chat.zekochat.PeptideApplication
import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.routes.account.EmailPasswordAssessment
import chat.zekochat.api.routes.account.negotiateAuthentication
import chat.zekochat.api.routes.onboard.needsOnboarding
import chat.zekochat.composables.generic.FormTextField
import chat.zekochat.composables.generic.SquareButton
import chat.zekochat.composables.generic.Weblink
import chat.zekochat.persistence.KVStorage
import chat.zekochat.ui.theme.FragmentMono
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val kvStorage: KVStorage
) : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
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
        isLoading = true

        viewModelScope.launch {
            try {
                val response = negotiateAuthentication(_email, _password)

                if (response.error != null) {
                    _error = response.error.type
                    isLoading = false
                    return@launch
                }

                Log.d("Login", "Checking for MFA")
                if (response.proceedMfa) {
                    Log.d("Login", "MFA required. Navigating to MFA screen")
                    _mfaResponse = response
                    _navigateTo = "mfa"
                    isLoading = false
                    return@launch
                }

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
                        isLoading = false
                        return@launch
                    }

                    PeptideAPI.loginAs(token)
                    PeptideAPI.setSessionId(response.firstUserHints.token)

                    _navigateTo = "home"
                } catch (e: Exception) {
                    _error = e.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                _error = if (e.message?.startsWith("Unexpected JSON token") == true) {
                    PeptideApplication.instance.getString(R.string.service_health_alert_body_default)
                } else e.message ?: "Unknown error"
            } finally {
                isLoading = false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel = hiltViewModel()) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as Activity).window
        val previousMode = window.attributes.softInputMode
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        onDispose { window.setSoftInputMode(previousMode) }
    }
    val passwordTextFieldState = rememberTextFieldState()
    LaunchedEffect(passwordTextFieldState.text) {
        viewModel.setPassword(passwordTextFieldState.text.toString())
    }
    val showPassword = remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = {},
                    actions = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                content = {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_arrow_back_24dp),
                                        contentDescription = stringResource(R.string.back)
                                    )
                                },
                                onClick = { navController.popBackStack() }
                            )
                            Text(
                                modifier = Modifier.clickable {
                                    navController.popBackStack()
                                    navController.navigate("register/details")
                                },
                                text = "Register"
                            )
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp, horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                modifier = Modifier
                    .size(120.dp),
                painter = painterResource(R.drawable.login_charachter_img),
                contentDescription = "Login character"
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.login_heading),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.login_subheading),
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

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                FormTextField(
                    value = viewModel.email,
                    label = stringResource(R.string.email),
                    type = KeyboardType.Email,
                    action = ImeAction.Next,
                    onChange = viewModel::setEmail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentType = ContentType.Password
                        }
                )
                Weblink(
                    text = stringResource(R.string.password_forgot),
                    url = "${PeptideAPI.getCurrentAppUrl()}/login/reset",
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                if (viewModel.error != null) {
                    Text(
                        text = viewModel.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))

                SquareButton(
                    onClick = {
                        viewModel.doLogin()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_platform_button"),
                    enabled = !viewModel.isLoading && viewModel.email.isNotBlank() && viewModel.password.isNotBlank()
                ) {
                    if (viewModel.isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(text = stringResource(R.string.login))
                    }
                }
            }
        }
    }
}
