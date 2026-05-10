package chat.stoat.composables.screens.settings

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import chat.stoat.R
import chat.stoat.screens.about.Library

@Composable
fun AttributionItem(library: Library, onClick: () -> Unit) {
    val context = LocalContext.current

    ListItem(
        headlineContent = {
            Text(
                text = library.name
            )
        },
        supportingContent = {
            Text(
                text = stringResource(id = R.string.oss_attribution_tap_to_view_license)
            )
        },
        trailingContent = {
            library.website?.let { website ->
                IconButton(onClick = {
                    val customTab = CustomTabsIntent
                        .Builder()
                        .build()
                    customTab.launchUrl(context, website.toUri())
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_open_in_new_24dp),
                        contentDescription = stringResource(id = R.string.oss_attribution_open_library_website)
                    )
                }
            }
        },
        modifier = Modifier
            .clickable(onClick = onClick)
    )
}
