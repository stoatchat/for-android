package chat.revolt.screens.chat.views.channel

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.settings.GeoStateProvider
import chat.revolt.composables.vectorassets.GeoGateUX

@Composable
fun ChannelScreenGeoGate(
    onAcknowledge: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Image(
            imageVector = GeoGateUX,
            contentDescription = null,
            modifier = Modifier.size(128.dp),
        )

        Text(
            text = stringResource(R.string.geogate_header),
            style = MaterialTheme.typography.titleMedium.copy(
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        when (GeoStateProvider.geoState?.countryCode) {
            "GB" -> {
                Text(
                    text = stringResource(R.string.geogate_description_variant_osa_uk_25),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            else -> {
                Text(
                    text = stringResource(R.string.geogate_description),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Button(onClick = { onAcknowledge() }) {
            Text(stringResource(R.string.geogate_acknowledge))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GeoGatePreview() {
    ChannelScreenGeoGate { }
}