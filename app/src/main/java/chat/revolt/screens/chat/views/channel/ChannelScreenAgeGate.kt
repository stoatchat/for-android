package chat.revolt.screens.chat.views.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.composables.generic.DobPicker
import chat.revolt.composables.generic.DobRegion
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable
fun ChannelScreenAgeGate(
    onAccept: () -> Unit,
    onDeny: () -> Unit
) {
    val context = LocalContext.current

    var dayValue by remember { mutableStateOf("") }
    var monthValue by remember { mutableStateOf("") }
    var yearValue by remember { mutableStateOf("") }

    var dobPickerError by remember { mutableStateOf("") }
    var dobValid by remember { mutableStateOf(false) }
    var dobPickerRegion by remember { mutableStateOf(DobRegion.European) }

    LaunchedEffect(Unit) {
        dobPickerRegion = when (Locale.current.region) {
            "US" -> DobRegion.American
            "GB" -> DobRegion.British
            "JP", "KR", "CN" -> DobRegion.ISO2601
            else -> DobRegion.European
        }
    }

    LaunchedEffect(dayValue, monthValue, yearValue) {
        dobValid = false
        if (dayValue.isNotBlank() && monthValue.isNotBlank() && yearValue.isNotBlank()) {
            val day = dayValue.toIntOrNull()
            val month = monthValue.toIntOrNull()
            val year = yearValue.toIntOrNull()

            // Invalid condition
            if (day == null || month == null || year == null) {
                dobPickerError = context.getString(R.string.channel_age_gate_dob_invalid)
                return@LaunchedEffect
            }

            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val then: LocalDateTime

            // Invalid date condition
            try {
                then = LocalDateTime(year, month, day, 0, 0, 0)
            } catch (e: Throwable) {
                dobPickerError = context.getString(R.string.channel_age_gate_dob_invalid)
                return@LaunchedEffect
            }

            // Too young condition (< 18)
            val diff =
                now.toInstant(UtcOffset.ZERO).epochSeconds - then.toInstant(UtcOffset.ZERO).epochSeconds
            if (diff < 568025136) {
                dobPickerError = context.getString(R.string.channel_age_gate_dob_too_young)
                return@LaunchedEffect
            }

            // Born in future condition
            if (then.toInstant(UtcOffset.ZERO).epochSeconds > now.toInstant(UtcOffset.ZERO).epochSeconds) {
                dobPickerError = context.getString(R.string.channel_age_gate_dob_invalid_future)
                return@LaunchedEffect
            }

            // Born before oldest person condition
            // Update from https://en.wikipedia.org/wiki/List_of_oldest_living_people
            val minYob = 1908
            if (year < minYob) {
                dobPickerError =
                    context.getString(R.string.channel_age_gate_dob_invalid_unlikely, minYob)
                return@LaunchedEffect
            }

            // Success condition
            dobPickerError = ""
            dobValid = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Icon(
            painter = painterResource(R.drawable.icn_eda_24dp),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )

        Text(
            text = stringResource(R.string.channel_age_gate_title),
            style = MaterialTheme.typography.titleMedium.copy(
                textAlign = TextAlign.Center
            ),
        )

        Text(
            text = stringResource(R.string.channel_age_gate_description),
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center
            ),
        )

        Text(
            text = dobPickerError,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
        )

        Text(
            text = stringResource(R.string.channel_age_gate_dob_section),
            style = MaterialTheme.typography.labelLarge
        )

        DobPicker(
            dayValue = dayValue,
            monthValue = monthValue,
            yearValue = yearValue,
            onDayChange = { dayValue = it },
            onMonthChange = { monthValue = it },
            onYearChange = { yearValue = it },
            order = dobPickerRegion.first,
            separator = dobPickerRegion.second,
        )

        Spacer(modifier = Modifier.width(0.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Button(onClick = { onDeny() }) {
                Text(stringResource(R.string.channel_age_gate_dob_cancel))
            }

            TextButton(onClick = { onAccept() }, enabled = dobValid) {
                Text(stringResource(R.string.channel_age_gate_dob_proceed))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AgeGateScreenPreview() {
    ChannelScreenAgeGate(onAccept = { /*TODO*/ }, onDeny = { /*TODO*/ })
}