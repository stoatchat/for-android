package chat.stoat.composables.generic

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Convenience wrapper around [TextField] that sets the [KeyboardOptions] and [VisualTransformation]
 */
@Composable
fun FormTextField(
    value: String,
    label: String,
    onChange: (it: String) -> Unit,
    modifier: Modifier = Modifier,
    type: KeyboardType = KeyboardType.Text,
    action: ImeAction = ImeAction.Done,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onChange,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = type, imeAction = action),
        visualTransformation = if (type == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None,
        label = { Text(label) },
        supportingText = supportingText,
        enabled = enabled,
        modifier = modifier,
        placeholder = placeholder
    )
}
