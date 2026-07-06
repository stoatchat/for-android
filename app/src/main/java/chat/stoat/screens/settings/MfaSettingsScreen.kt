package chat.stoat.screens.settings

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.routes.account.MfaSettings
import chat.stoat.api.routes.account.disableTotp
import chat.stoat.api.routes.account.enableTotp
import chat.stoat.api.routes.account.fetchAccount
import chat.stoat.api.routes.account.fetchMfaSettings
import chat.stoat.api.routes.account.fetchRecoveryCodes
import chat.stoat.api.routes.account.generateTotpSecret
import chat.stoat.api.routes.account.regenerateRecoveryCodes
import chat.stoat.composables.mfa.MfaPromptSheet
import chat.stoat.composables.mfa.MfaPromptState
import chat.stoat.composables.mfa.RecoveryCodesDialog
import chat.stoat.composables.mfa.mfaErrorText
import chat.stoat.composables.vectorassets.MfaOff
import chat.stoat.composables.vectorassets.MfaOn
import chat.stoat.core.model.data.STOAT_WEB_APP
import chat.stoat.internals.Platform
import chat.stoat.settings.dsl.SettingsPage
import chat.stoat.ui.theme.FragmentMono
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@Stable
class TotpEnrollment(val secret: String, val otpauthUrl: String) {
    val input = TextFieldState()
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
}

class MfaSettingsScreenViewModel(val context: Application) : ViewModel() {
    val mfaPrompt = MfaPromptState()

    var mfaStateLoaded by mutableStateOf(false)
        private set
    var mfaState by mutableStateOf<MfaSettings?>(null)
        private set

    var actionBusy by mutableStateOf(false)
        private set

    var actionError by mutableStateOf<String?>(null)
        private set

    var snackbarMessage by mutableStateOf<Int?>(null)

    var displayedRecoveryCodes by mutableStateOf<List<String>?>(null)

    var totpEnrollment by mutableStateOf<TotpEnrollment?>(null)
        private set

    suspend fun loadDetails() {
        runCatching { fetchMfaSettings() }
            .onSuccess { mfaState = it }
            .also { mfaStateLoaded = true }
    }

    private fun withMfaTicket(block: suspend (mfaTicketToken: String) -> Unit) {
        viewModelScope.launch {
            if (actionBusy) return@launch
            actionError = null

            val ticket = mfaPrompt.request() ?: return@launch

            actionBusy = true
            runCatching { block(ticket.token) }
                .onFailure { actionError = it.message }
            actionBusy = false
        }
    }

    fun disableAuthenticator() = withMfaTicket { token ->
        disableTotp(token)
        loadDetails()
        snackbarMessage = R.string.settings_mfa_totp_disabled_snackbar
    }

    fun showRecoveryCodes() = withMfaTicket { token ->
        displayedRecoveryCodes = fetchRecoveryCodes(token)
    }

    fun rotateRecoveryCodes() = withMfaTicket { token ->
        displayedRecoveryCodes = regenerateRecoveryCodes(token)
        loadDetails()
    }

    fun beginTotpEnrollment() = withMfaTicket { token ->
        val secret = generateTotpSecret(token)
        val email = runCatching { fetchAccount().email }.getOrNull()
        totpEnrollment = TotpEnrollment(secret, otpauthUrl(secret, email))
    }

    fun confirmTotpEnrollment() {
        val enrollment = totpEnrollment ?: return
        if (enrollment.busy) return

        viewModelScope.launch {
            enrollment.busy = true
            enrollment.error = null

            runCatching { enableTotp(enrollment.input.text.toString()) }
                .onFailure {
                    enrollment.error = it.message
                    enrollment.busy = false
                }
                .onSuccess {
                    loadDetails()
                    totpEnrollment = null
                    snackbarMessage = R.string.settings_mfa_totp_enabled_snackbar
                }
        }
    }

    fun dismissTotpEnrollment() {
        if (totpEnrollment?.busy != true) totpEnrollment = null
    }

    private fun otpauthUrl(secret: String, email: String?): String {
        val label = Uri.encode("Stoat:${email ?: "account"}")
        return "otpauth://totp/$label?secret=$secret&issuer=Stoat"
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MfaSettingsScreen(
    navController: NavController,
    viewModel: MfaSettingsScreenViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.loadDetails()
    }

    MfaPromptSheet(viewModel.mfaPrompt)

    viewModel.displayedRecoveryCodes?.let { codes ->
        RecoveryCodesDialog(codes) { viewModel.displayedRecoveryCodes = null }
    }

    viewModel.totpEnrollment?.let { enrollment ->
        TotpEnrollmentSheet(
            enrollment = enrollment,
            onConfirm = { viewModel.confirmTotpEnrollment() },
            onDismiss = { viewModel.dismissTotpEnrollment() }
        )
    }

    SettingsPage(
        navController,
        title = { Text(stringResource(R.string.settings_mfa)) }
    ) {
        LaunchedEffect(viewModel.snackbarMessage) {
            viewModel.snackbarMessage?.let {
                showSnackbar(resources.getString(it))
                viewModel.snackbarMessage = null
            }
        }

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
                Column {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 22.dp, vertical = 18.dp)
                    ) {
                        val mfaConsideredEnabled =
                            viewModel.mfaState?.totpMfa == true || viewModel.mfaState?.securityKeyMfa == true

                        Image(
                            imageVector = if (mfaConsideredEnabled) MfaOn else MfaOff,
                            contentDescription = null,
                            modifier = Modifier
                                .height(128.dp)
                                .padding(bottom = 16.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_mfa_status),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        if (mfaConsideredEnabled) {
                            Text(
                                text = stringResource(R.string.settings_mfa_status_enabled),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.settings_mfa_status_not_set_up),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = stringResource(R.string.settings_mfa_status_not_set_up_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    AnimatedContent(targetState = viewModel.actionError) { error ->
                        if (error != null) {
                            Text(
                                text = mfaErrorText(error),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                                    .padding(top = 12.dp)
                            )
                        }
                    }

                    if (viewModel.mfaState?.totpMfa == true) {
                        Subcategory(
                            title = { Text(stringResource(R.string.settings_mfa_disable_totp)) }
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_mfa_disable_totp_description),
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Button(
                                    onClick = { viewModel.disableAuthenticator() },
                                    enabled = !viewModel.actionBusy,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text(stringResource(R.string.settings_mfa_disable_totp_cta))
                                }
                            }
                        }
                    } else {
                        Subcategory(
                            title = { Text(stringResource(R.string.settings_mfa_totp)) }
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_mfa_enable_totp_description),
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Button(
                                    onClick = { viewModel.beginTotpEnrollment() },
                                    enabled = !viewModel.actionBusy
                                ) {
                                    Text(stringResource(R.string.settings_mfa_enable_totp_cta))
                                }
                            }
                        }
                    }

                    Subcategory(
                        title = { Text(stringResource(R.string.settings_mfa_recovery)) }
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            val recoveryActive = viewModel.mfaState?.recoveryActive == true

                            Text(
                                text = stringResource(
                                    if (recoveryActive) {
                                        R.string.settings_mfa_recovery_description_active
                                    } else {
                                        R.string.settings_mfa_recovery_description_inactive
                                    }
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (recoveryActive) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.showRecoveryCodes() },
                                        enabled = !viewModel.actionBusy
                                    ) {
                                        Text(stringResource(R.string.settings_mfa_recovery_view_cta))
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.rotateRecoveryCodes() },
                                        enabled = !viewModel.actionBusy
                                    ) {
                                        Text(stringResource(R.string.settings_mfa_recovery_regenerate_cta))
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.rotateRecoveryCodes() },
                                    enabled = !viewModel.actionBusy
                                ) {
                                    Text(stringResource(R.string.settings_mfa_recovery_generate_cta))
                                }
                            }
                        }
                    }

                    if (viewModel.mfaState?.securityKeyMfa == true) {
                        Subcategory(
                            title = { Text(stringResource(R.string.settings_mfa_security_keys)) }
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_mfa_security_keys_web_manage_description),
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Button(
                                    onClick = {
                                        val customTab = CustomTabsIntent
                                            .Builder()
                                            .build()
                                        customTab.launchUrl(context, "$STOAT_WEB_APP/app".toUri())
                                    }
                                ) {
                                    Text(stringResource(R.string.settings_mfa_security_keys_web_manage_cta))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TotpEnrollmentSheet(
    enrollment: TotpEnrollment,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val clipboardManager = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = { if (!enrollment.busy) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = false
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .imePadding()
        ) {
            Text(
                text = stringResource(R.string.settings_mfa_totp_setup_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.settings_mfa_totp_setup_scan),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            TotpQrCode(enrollment.otpauthUrl)

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_mfa_totp_setup_manual),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = enrollment.secret,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FragmentMono
                        )
                    )
                }

                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(enrollment.secret))
                    if (Platform.needsShowClipboardNotification()) {
                        Toast.makeText(
                            context,
                            resources.getString(R.string.copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_content_copy_24dp),
                        contentDescription = stringResource(R.string.settings_mfa_totp_setup_copy_secret)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_mfa_totp_setup_code_lead),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            val transformation = remember {
                InputTransformation {
                    val current = asCharSequence().toString()
                    val sanitized = current.filter(Char::isDigit).take(6)
                    if (sanitized != current) replace(0, length, sanitized)
                }
            }

            TextField(
                state = enrollment.input,
                label = { Text(stringResource(R.string.settings_mfa_totp_setup_code_label)) },
                inputTransformation = transformation,
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                onKeyboardAction = { onConfirm() },
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FragmentMono,
                    letterSpacing = 2.sp
                ),
                enabled = !enrollment.busy,
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedContent(targetState = enrollment.error) { error ->
                if (error != null) {
                    Text(
                        text = mfaErrorText(error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !enrollment.busy
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = onConfirm,
                    enabled = !enrollment.busy && enrollment.input.text.length == 6
                ) {
                    AnimatedContent(targetState = enrollment.busy) { busy ->
                        if (busy) {
                            LoadingIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text(stringResource(R.string.settings_mfa_totp_setup_verify))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TotpQrCode(contents: String) {
    var qrCode by remember(contents) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(contents) {
        qrCode = withContext(Dispatchers.Default) {
            val matrix = QRCodeWriter().encode(
                contents,
                BarcodeFormat.QR_CODE,
                512,
                512,
                mapOf(EncodeHintType.MARGIN to "1")
            )

            val bitmap = createBitmap(matrix.width, matrix.height)

            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    bitmap[x, y] = if (matrix.get(x, y)) {
                        android.graphics.Color.BLACK
                    } else {
                        android.graphics.Color.WHITE
                    }
                }
            }

            bitmap.asImageBitmap()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(220.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White)
            .padding(12.dp)
    ) {
        qrCode?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize()
            )
        } ?: LoadingIndicator(modifier = Modifier.size(48.dp))
    }
}
