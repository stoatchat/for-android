package chat.stoat.screens.settings

import android.app.Application
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.routes.account.MfaSettings
import chat.stoat.api.routes.account.changeEmail
import chat.stoat.api.routes.account.changePassword
import chat.stoat.api.routes.account.fetchAccount
import chat.stoat.api.routes.account.fetchMfaSettings
import chat.stoat.settings.dsl.SettingsPage
import chat.stoat.ui.theme.FragmentMono
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class AccountSettingsScreenViewModel(val context: Application) : ViewModel() {
    var accountEmail by mutableStateOf("")
        private set
    var accountEmailMasked by mutableStateOf("")
        private set

    var accountEmailLoaded by mutableStateOf(false)
        private set
    var mfaStateLoaded by mutableStateOf(false)
        private set

    var editingEmail by mutableStateOf(false)
    var emailChangeError by mutableStateOf<String?>(null)
    var waitingForEmailChangeNetworkResponse by mutableStateOf(false)
    var showEmailChangeSuccess by mutableStateOf(false)

    var editingPassword by mutableStateOf(false)
    var passwordChangeError by mutableStateOf<String?>(null)
    var waitingForPasswordChangeNetworkResponse by mutableStateOf(false)
    var showSnackbarPasswordChanged by mutableIntStateOf(0)

    var mfaState by mutableStateOf<MfaSettings?>(null)
        private set

    suspend fun loadDetails() {
        runCatching { fetchAccount() }
            .onSuccess { account ->
                accountEmail = account.email
                val parts = account.email.split("@")
                if (parts.size == 2) {
                    val domainPart = parts[1]
                    val bullets = "\u2022".repeat(5)
                    accountEmailMasked = "$bullets@$domainPart"
                } else {
                    accountEmailMasked = account.email
                }
            }
            .onFailure {
                emailChangeError = context.getString(R.string.settings_account_email_loading_error)
            }
            .also { accountEmailLoaded = true }

        runCatching { fetchMfaSettings() }
            .onSuccess { mfaState = it }
            .also { mfaStateLoaded = true }
    }

    fun requestChangeEmail(newEmail: CharSequence, password: CharSequence) {
        viewModelScope.launch {
            waitingForEmailChangeNetworkResponse = true
            runCatching { changeEmail(newEmail.toString(), password.toString()) }
                .onSuccess {
                    editingEmail = false
                    loadDetails()
                    emailChangeError = null
                    showEmailChangeSuccess = true
                }
                .onFailure { e ->
                    editingEmail = false
                    passwordChangeError = null
                    emailChangeError = e.message ?: "Unknown error"
                }
                .also { waitingForEmailChangeNetworkResponse = false }
        }
    }

    fun requestChangePassword(newPassword: CharSequence, currentPassword: CharSequence) {
        viewModelScope.launch {
            waitingForPasswordChangeNetworkResponse = true
            runCatching { changePassword(newPassword.toString(), currentPassword.toString()) }
                .onSuccess {
                    editingPassword = false
                    passwordChangeError = null
                    showSnackbarPasswordChanged++
                }
                .onFailure { e ->
                    editingPassword = false
                    emailChangeError = null
                    passwordChangeError = e.message ?: "Unknown error"
                }
                .also { waitingForPasswordChangeNetworkResponse = false }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountSettingsScreen(
    navController: NavController,
    viewModel: AccountSettingsScreenViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.loadDetails()
    }

    if (viewModel.showEmailChangeSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.showEmailChangeSuccess = false },
            title = { Text(stringResource(R.string.settings_account_email_confirm_email)) },
            text = { Text(stringResource(R.string.settings_account_email_confirm_email_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_APP_EMAIL)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)

                    viewModel.showEmailChangeSuccess = false
                }) {
                    Text(stringResource(R.string.settings_account_email_confirm_email_open_mail_app))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showEmailChangeSuccess = false }) {
                    Text(stringResource(R.string.settings_account_email_confirm_email_dismiss))
                }
            }
        )
    }

    if (viewModel.editingEmail) {
        val newEmailFieldState = rememberTextFieldState()
        val confirmationPasswordFieldState = rememberTextFieldState()

        AlertDialog(
            onDismissRequest = {
                if (!viewModel.waitingForEmailChangeNetworkResponse) viewModel.editingEmail = false
            },
            title = { Text(stringResource(R.string.settings_account_email_edit_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(
                            R.string.settings_account_email_edit_current,
                            viewModel.accountEmail
                        )
                    )
                    Text(stringResource(R.string.settings_account_email_edit_message))
                    TextField(
                        state = newEmailFieldState,
                        label = { Text(stringResource(R.string.settings_account_email_edit_new_email)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.EmailAddress }
                    )
                    SecureTextField(
                        state = confirmationPasswordFieldState,
                        label = { Text(stringResource(R.string.settings_account_email_edit_password)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.Password }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.requestChangeEmail(
                            newEmailFieldState.text,
                            confirmationPasswordFieldState.text
                        )
                    },
                    enabled = !viewModel.waitingForEmailChangeNetworkResponse &&
                            newEmailFieldState.text.isNotBlank() &&
                            confirmationPasswordFieldState.text.isNotBlank()
                ) {
                    Text(stringResource(R.string.settings_account_email_edit_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.editingEmail = false }) {
                    Text(stringResource(R.string.settings_account_email_edit_cancel))
                }
            }
        )
    }

    if (viewModel.editingPassword) {
        val confirmationPasswordFieldState = rememberTextFieldState()
        val newPasswordFieldState = rememberTextFieldState()

        AlertDialog(
            onDismissRequest = {
                if (!viewModel.waitingForPasswordChangeNetworkResponse) viewModel.editingPassword =
                    false
            },
            title = { Text(stringResource(R.string.settings_account_password_edit_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.settings_account_password_edit_message))
                    Text(stringResource(R.string.settings_account_password_edit_message_hint))
                    SecureTextField(
                        state = confirmationPasswordFieldState,
                        label = { Text(stringResource(R.string.settings_account_password_edit_current_password)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.Password }
                    )
                    SecureTextField(
                        state = newPasswordFieldState,
                        label = { Text(stringResource(R.string.settings_account_password_edit_new_password)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.NewPassword }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.requestChangePassword(
                            newPasswordFieldState.text,
                            confirmationPasswordFieldState.text
                        )
                    },
                    enabled = !viewModel.waitingForPasswordChangeNetworkResponse &&
                            newPasswordFieldState.text.isNotBlank() &&
                            confirmationPasswordFieldState.text.isNotBlank() &&
                            newPasswordFieldState.text != confirmationPasswordFieldState.text &&
                            newPasswordFieldState.text.length >= 8
                ) {
                    Text(stringResource(R.string.settings_account_password_edit_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.editingPassword = false }) {
                    Text(stringResource(R.string.settings_account_password_edit_cancel))
                }
            }
        )
    }

    SettingsPage(
        navController,
        title = { Text(stringResource(R.string.settings_account)) }
    ) {
        LaunchedEffect(viewModel.showSnackbarPasswordChanged) {
            if (viewModel.showSnackbarPasswordChanged > 0) {
                showSnackbar(resources.getString(R.string.settings_account_password_change_success))
            }
        }

        AnimatedContent(
            targetState = viewModel.accountEmailLoaded
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
                Column {
                    AnimatedVisibility(
                        viewModel.emailChangeError != null
                                || viewModel.passwordChangeError != null
                    ) {
                        Text(
                            text = viewModel.emailChangeError ?: viewModel.passwordChangeError
                            ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Subcategory(
                        title = { Text(stringResource(R.string.settings_account_email)) }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable { viewModel.editingEmail = true }
                                .padding(horizontal = 22.dp, vertical = 18.dp)
                        ) {
                            Text(
                                text = viewModel.accountEmailMasked,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_edit_24dp),
                                contentDescription = stringResource(R.string.settings_account_email_edit_alt)
                            )
                        }
                    }

                    Subcategory(
                        title = { Text(stringResource(R.string.settings_account_password)) }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable { viewModel.editingPassword = true }
                                .padding(horizontal = 22.dp, vertical = 18.dp)
                        ) {
                            Text(
                                text = remember { "\u2022".repeat(11) },
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FragmentMono,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_edit_24dp),
                                contentDescription = stringResource(R.string.settings_account_password_edit_alt)
                            )
                        }
                    }

                    Subcategory(
                        title = { Text(stringResource(R.string.settings_account_mfa)) }
                    ) {
                        AnimatedVisibility(
                            viewModel.mfaStateLoaded && viewModel.mfaState?.totpMfa == false
                        ) {
                            Text(
                                text = stringResource(R.string.settings_account_mfa_warning),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(16.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable {
                                    navController.navigate("settings/account/mfa")
                                }
                                .padding(horizontal = 22.dp, vertical = 18.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_account_mfa_totp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.settings_account_mfa_totp_upsell),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = viewModel.mfaState?.totpMfa == true || viewModel.mfaState?.securityKeyMfa == true,
                                onCheckedChange = null
                            )
                        }
                    }
                }
            }
        }
    }
}