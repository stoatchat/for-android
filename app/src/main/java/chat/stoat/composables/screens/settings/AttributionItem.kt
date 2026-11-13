package chat.stoat.composables.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import chat.stoat.core.designsystem.icons.StoatIcons
import chat.stoat.core.designsystem.icons.painter
import chat.stoat.screens.about.Library
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.oss_attribution_open_library_website
import chat.stoat.shared.resources.oss_attribution_tap_to_view_license
import org.jetbrains.compose.resources.stringResource

@Composable
fun AttributionItem(library: Library, onClick: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    ListItem(
        supportingContent = {
            Text(
                text = stringResource(Res.string.oss_attribution_tap_to_view_license)
            )
        },
        trailingContent = {
            library.website?.let { website ->
                IconButton(onClick = { uriHandler.openUri(website) }) {
                    Icon(
                        painter = StoatIcons.OpenExternal.painter(),
                        contentDescription = stringResource(Res.string.oss_attribution_open_library_website)
                    )
                }
            }
        },
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Text(
            text = library.name
        )
    }
}
