package chat.peptide.composables.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import chat.peptide.R
import chat.peptide.screens.about.Library

@Composable
fun AttributionItem(library: Library, onClick: () -> Unit) {
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
        modifier = Modifier
            .clickable(onClick = onClick)
    )
}
