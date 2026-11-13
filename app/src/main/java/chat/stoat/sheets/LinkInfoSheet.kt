package chat.stoat.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import chat.stoat.core.designsystem.components.SheetButton
import chat.stoat.core.designsystem.icons.StoatIcons
import chat.stoat.core.designsystem.icons.painter
import chat.stoat.internals.Platform
import chat.stoat.platform.LocalToaster
import chat.stoat.platform.show
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.copied
import chat.stoat.shared.resources.copy
import chat.stoat.shared.resources.link_open
import chat.stoat.shared.resources.link_type_no_intent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun LinkInfoSheet(url: String, onDismiss: () -> Unit) {
    val linkTypeNoIntentText = stringResource(Res.string.link_type_no_intent)
    val clipboardManager = LocalClipboardManager.current
    val toaster = LocalToaster.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember(::MutableInteractionSource),
                    indication = null,
                    onClick = {
                        try {
                            uriHandler.openUri(url)
                        } catch (_: IllegalArgumentException) {
                            toaster.show(linkTypeNoIntentText)
                        }
                    })
        ) {
            Text(
                text = url,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider()
    }

    SheetButton(
        headlineContent = {
            Text(
                text = stringResource(Res.string.link_open)
            )
        },
        leadingContent = {
            Icon(
                painter = StoatIcons.OpenInBrowser.painter(),
                contentDescription = null
            )
        },
        onClick = {
            try {
                uriHandler.openUri(url)
            } catch (_: IllegalArgumentException) {
                toaster.show(linkTypeNoIntentText)
            }
        }
    )
    SheetButton(
        headlineContent = {
            Text(
                text = stringResource(Res.string.copy)
            )
        },
        leadingContent = {
            Icon(
                painter = StoatIcons.Copy.painter(),
                contentDescription = null
            )
        },
        onClick = {
            coroutineScope.launch {
                clipboardManager.setText(AnnotatedString(url))
                if (Platform.needsShowClipboardNotification()) {
                    toaster.show(getString(Res.string.copied))
                }
            }
            onDismiss()
        }
    )


}
