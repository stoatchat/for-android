package chat.stoat.composables.mfa

import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.KeyboardType
import chat.stoat.api.routes.account.MfaResponse
import chat.stoat.api.routes.account.MfaResponsePassword
import chat.stoat.api.routes.account.MfaResponseRecoveryCode
import chat.stoat.api.routes.account.MfaResponseTotpCode
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.mfa_method_password
import chat.stoat.shared.resources.mfa_method_recovery
import chat.stoat.shared.resources.mfa_method_totp
import chat.stoat.shared.resources.mfa_password_lead
import chat.stoat.shared.resources.mfa_recovery_code
import chat.stoat.shared.resources.mfa_recovery_lead
import chat.stoat.shared.resources.mfa_totp_code
import chat.stoat.shared.resources.mfa_totp_lead
import org.jetbrains.compose.resources.StringResource

enum class MfaMethod(
    val apiName: String,
    val label: StringResource,
    val lead: StringResource,
    val inputLabel: StringResource,
    val keyboardType: KeyboardType,
    val secureInput: Boolean,
    val monospaceInput: Boolean,
    val autofillContentType: ContentType?,
    /** Submit without pressing confirm as soon as [isSubmittable] returns true */
    val autoSubmit: Boolean,
    val sanitizeInput: (String) -> String,
    val isSubmittable: (String) -> Boolean,
    val buildResponse: (String) -> MfaResponse
) {
    Totp(
        apiName = "Totp",
        label = Res.string.mfa_method_totp,
        lead = Res.string.mfa_totp_lead,
        inputLabel = Res.string.mfa_totp_code,
        keyboardType = KeyboardType.Number,
        secureInput = false,
        monospaceInput = true,
        autofillContentType = null,
        autoSubmit = true,
        sanitizeInput = { input -> input.filter(Char::isDigit).take(6) },
        isSubmittable = { it.length == 6 },
        buildResponse = { MfaResponseTotpCode(it) }
    ),

    Password(
        apiName = "Password",
        label = Res.string.mfa_method_password,
        lead = Res.string.mfa_password_lead,
        inputLabel = Res.string.mfa_method_password,
        keyboardType = KeyboardType.Password,
        secureInput = true,
        monospaceInput = false,
        autofillContentType = ContentType.Password,
        autoSubmit = false,
        sanitizeInput = { it },
        isSubmittable = { it.isNotEmpty() },
        buildResponse = { MfaResponsePassword(it) }
    ),

    Recovery(
        apiName = "Recovery",
        label = Res.string.mfa_method_recovery,
        lead = Res.string.mfa_recovery_lead,
        inputLabel = Res.string.mfa_recovery_code,
        keyboardType = KeyboardType.Text,
        secureInput = false,
        monospaceInput = true,
        autofillContentType = null,
        autoSubmit = false,
        sanitizeInput = { it.trim() },
        isSubmittable = { it.isNotBlank() },
        buildResponse = { MfaResponseRecoveryCode(it) }
    );

    companion object {
        fun fromApiName(name: String): MfaMethod? = entries.firstOrNull { it.apiName == name }
    }
}
