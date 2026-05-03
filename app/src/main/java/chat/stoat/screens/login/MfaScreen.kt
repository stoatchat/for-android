package chat.stoat.screens.login

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.account.MfaResponseRecoveryCode
import chat.stoat.api.routes.account.MfaResponseTotpCode
import chat.stoat.api.routes.account.authenticateWithMfaRecoveryCode
import chat.stoat.api.routes.account.authenticateWithMfaTotpCode
import chat.stoat.composables.generic.CollapsibleCard
import chat.stoat.composables.generic.FormTextField
import chat.stoat.persistence.KVStorage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class MfaScreenViewModel(
    private val kvStorage: KVStorage
) : ViewModel() {
    private var _totpCode by mutableStateOf("")
    val totpCode: String
        get() = _totpCode

    private var _recoveryCode by mutableStateOf("")
    val recoveryCode: String
        get() = _recoveryCode

    private var _error by mutableStateOf<String?>(null)
    val error: String?
        get() = _error

    private var _navigateToHome by mutableStateOf(false)
    val navigateToHome: Boolean
        get() = _navigateToHome

    fun setTotpCode(code: String) {
        _totpCode = code.replace(Regex("[^0-9]"), "")
    }

    fun setRecoveryCode(code: String) {
        _recoveryCode = code
    }

    fun tryAuthorizeTotp(mfaTicket: String) {
        _error = null
        viewModelScope.launch {
            val response = authenticateWithMfaTotpCode(mfaTicket, MfaResponseTotpCode(totpCode))
            if (response.error != null) {
                _error = response.error.type
            } else {
                Log.d(
                    "MFA",
                    "Successfully authorized with TOTP."
                )

                try {
                    val token = response.firstUserHints!!.token
                    val id = response.firstUserHints.id

                    StoatAPI.loginAs(token)
                    StoatAPI.setSessionId(id)
                    kvStorage.set("sessionToken", token)
                    kvStorage.set("sessionId", id)

                    _navigateToHome = true
                } catch (e: Error) {
                    _error = e.message ?: "Unknown error"
                }
            }
        }
    }

    fun tryAuthorizeRecovery(mfaTicket: String) {
        _error = null
        viewModelScope.launch {
            val response =
                authenticateWithMfaRecoveryCode(mfaTicket, MfaResponseRecoveryCode(recoveryCode))
            if (response.error != null) {
                _error = response.error.type
            } else {
                Log.d(
                    "MFA",
                    "Successfully authorized with a recovery code."
                )

                try {
                    val token = response.firstUserHints!!.token
                    val id = response.firstUserHints.id

                    StoatAPI.loginAs(token)
                    StoatAPI.setSessionId(id)
                    kvStorage.set("sessionToken", token)
                    kvStorage.set("sessionId", id)

                    _navigateToHome = true
                } catch (e: Error) {
                    _error = e.message ?: "Unknown error"
                }
            }
        }
    }
}

@Composable
fun MfaScreen(
    navController: NavController,
    allowedAuthTypesCommaSep: String,
    mfaTicket: String,
    viewModel: MfaScreenViewModel = koinViewModel()
) {
    val allowedAuthTypes = allowedAuthTypesCommaSep.split(",")

    LaunchedEffect(viewModel.navigateToHome) {
        if (viewModel.navigateToHome) {
            navController.navigate("chat") {
                popUpTo("login/greeting") { inclusive = true }
            }
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
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.mfa_interstitial_header),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.mfa_interstitial_lead),
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.5f
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth()
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
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .fillMaxWidth()
                )
            }

            // Collapsible cards for each auth type
            allowedAuthTypes.forEach { authType ->
                when (authType) {
                    "Totp" -> {
                        CollapsibleCard(header = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ux_mfa_totp),
                                    contentDescription = null, // See label
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(end = 10.dp)
                                )
                                Text(stringResource(R.string.mfa_totp_header))
                            }
                        }) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.mfa_totp_lead),
                                    color = MaterialTheme.colorScheme.onBackground.copy(
                                        alpha = 0.5f
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .fillMaxWidth()
                                )

                                FormTextField(
                                    label = stringResource(R.string.mfa_totp_code),
                                    onChange = viewModel::setTotpCode,
                                    type = KeyboardType.Number,
                                    value = viewModel.totpCode
                                )

                                Button(
                                    onClick = { viewModel.tryAuthorizeTotp(mfaTicket) },
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .fillMaxWidth()
                                        .testTag("do_totp_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.next)
                                    )
                                }
                            }
                        }
                    }

                    "Recovery" -> {
                        CollapsibleCard(header = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ux_mfa_restore),
                                    contentDescription = null, // See label
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(end = 10.dp)
                                )
                                Text(stringResource(R.string.mfa_recovery_header))
                            }
                        }) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.mfa_recovery_lead),
                                    color = MaterialTheme.colorScheme.onBackground.copy(
                                        alpha = 0.5f
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .fillMaxWidth()
                                )

                                FormTextField(
                                    label = stringResource(R.string.mfa_recovery_code),
                                    onChange = viewModel::setRecoveryCode,
                                    value = viewModel.recoveryCode
                                )

                                Button(
                                    onClick = { viewModel.tryAuthorizeRecovery(mfaTicket) },
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .fillMaxWidth()
                                        .testTag("do_mfa_recovery_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.next)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ElevatedButton(
            onClick = { navController.popBackStack() }
        ) {
            Text(text = stringResource(R.string.cancel))
        }
    }
}
