package chat.revolt.screens.register

import android.content.Context
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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.RevoltApplication
import chat.revolt.api.routes.account.RegistrationBody
import chat.revolt.api.routes.account.register
import chat.revolt.api.routes.misc.getRootRoute
import chat.revolt.composables.generic.FormTextField
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaSize
import com.hcaptcha.sdk.HCaptchaTheme
import kotlinx.coroutines.launch

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
                    RevoltApplication.instance.getString(R.string.service_health_alert_body_default)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterDetailsScreen(
    navController: NavController,
    viewModel: RegisterDetailsScreenViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                    navController.navigate("login/login")
                                },
                                text = stringResource(R.string.login)
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
                .padding(vertical = 20.dp, horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                modifier = Modifier
                    .size(120.dp),
                painter = painterResource(R.drawable.register_happy_img),
                contentDescription = "Login character"
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.register_form_heading),
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
                text = stringResource(R.string.register_data),
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
                    onChange = { viewModel.email = it },
                    label = stringResource(R.string.register_email),
                    type = KeyboardType.Email,
                    action = ImeAction.Next,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.EmailAddress }
                )
                Spacer(modifier = Modifier.height(16.dp))
                FormTextField(
                    value = viewModel.password,
                    onChange = { viewModel.password = it },
                    label = stringResource(R.string.register_password),
                    type = KeyboardType.Email,
                    action = ImeAction.Next,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.EmailAddress }
                )
                Spacer(modifier = Modifier.height(32.dp))

                val context = LocalContext.current

                Button(
                    onClick = {
                        viewModel.initCaptcha(context) {
                            viewModel.doRegistration(navController)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_continue_button"),
                    shape = MaterialTheme.shapes.small.copy(
                        topStart = CornerSize(8.dp),
                        topEnd = CornerSize(8.dp),
                        bottomStart = CornerSize(8.dp),
                        bottomEnd = CornerSize(8.dp)
                    )
                ) {
                    Text(text = stringResource(R.string.continue_))
                }
            }
        }
    }
}
