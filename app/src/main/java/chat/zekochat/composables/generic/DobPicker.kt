package chat.zekochat.composables.generic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.zekochat.R

sealed class DobElement {
    data object Day : DobElement()
    data object Month : DobElement()
    data object Year : DobElement()
}

object DobRegion {
    val American = listOf(DobElement.Month, DobElement.Day, DobElement.Year) to "/"
    val British = listOf(DobElement.Day, DobElement.Month, DobElement.Year) to "/"
    val European = listOf(DobElement.Day, DobElement.Month, DobElement.Year) to "."
    val ISO2601 = listOf(DobElement.Year, DobElement.Month, DobElement.Day) to "-"
}

@Composable
fun DobPicker(
    dayValue: String,
    monthValue: String,
    yearValue: String,
    onDayChange: (String) -> Unit,
    onMonthChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    order: List<DobElement> = DobRegion.European.first,
    separator: String = DobRegion.European.second,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        order.forEach { element ->
            when (element) {
                DobElement.Day -> DobPickerDay(dayValue, onDayChange)
                DobElement.Month -> DobPickerMonth(monthValue, onMonthChange)
                DobElement.Year -> DobPickerYear(yearValue, onYearChange)
            }

            if (element != order.last()) {
                Text(
                    text = separator,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun DobPickerDay(
    dayValue: String,
    onDayChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.channel_age_gate_dob_day),
            style = MaterialTheme.typography.labelMedium,
        )
        OutlinedTextField(
            value = dayValue,
            onValueChange = {
                if (it.length <= 2) {
                    onDayChange(it)
                } else {
                    focusManager.moveFocus(FocusDirection.Next)
                }

                if (it.length == 2) {
                    focusManager.moveFocus(FocusDirection.Next)
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFeatureSettings = "tnum",
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(56.dp)
        )
    }
}

@Composable
private fun DobPickerMonth(
    monthValue: String,
    onMonthChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current


    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.channel_age_gate_dob_month),
            style = MaterialTheme.typography.labelMedium,
        )
        OutlinedTextField(
            value = monthValue,
            onValueChange = {
                if (it.length <= 2) {
                    onMonthChange(it)
                }

                if (it.length == 2) {
                    focusManager.moveFocus(FocusDirection.Next)
                } else if (it.isEmpty()) {
                    focusManager.moveFocus(FocusDirection.Previous)
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFeatureSettings = "tnum",
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(56.dp),
        )
    }
}

@Composable
private fun DobPickerYear(
    yearValue: String,
    onYearChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.channel_age_gate_dob_year),
            style = MaterialTheme.typography.labelMedium,
        )
        OutlinedTextField(
            value = yearValue,
            onValueChange = {
                onYearChange(it)
                if (it.isEmpty()) {
                    focusManager.moveFocus(FocusDirection.Previous)
                } else if (it.length == 4) {
                    focusManager.clearFocus()
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFeatureSettings = "tnum",
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(78.dp),
        )
    }
}