package chat.stoat.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.core.designsystem.icons.StoatIcons
import chat.stoat.core.designsystem.icons.painter
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.spark_early_access
import chat.stoat.shared.resources.spark_early_access_cta
import chat.stoat.shared.resources.spark_early_access_description_1
import chat.stoat.shared.resources.spark_early_access_description_2
import org.jetbrains.compose.resources.stringResource

@Composable
fun EarlyAccessSheet(onClose: () -> Unit) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(horizontal = 8.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = StoatIcons.EarlyAccess.painter(),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .padding(vertical = 8.dp)
                .size(64.dp)
        )
        Text(
            stringResource(Res.string.spark_early_access),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp),
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(Res.string.spark_early_access_description_1),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(Res.string.spark_early_access_description_2),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.spark_early_access_cta))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EarlyAccessSheetPreview() {
    EarlyAccessSheet { }
}