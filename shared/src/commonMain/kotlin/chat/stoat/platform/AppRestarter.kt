package chat.stoat.platform

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberAppRestarter(): () -> Unit
