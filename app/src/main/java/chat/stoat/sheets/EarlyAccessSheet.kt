package chat.stoat.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R

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
            painter = painterResource(R.drawable.ux_early_access),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .padding(vertical = 8.dp)
        )
        Text(
            stringResource(R.string.spark_early_access),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp),
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.spark_early_access_description_1),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.spark_early_access_description_2),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.spark_early_access_description_3),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.spark_early_access_cta))
        }
    }
}