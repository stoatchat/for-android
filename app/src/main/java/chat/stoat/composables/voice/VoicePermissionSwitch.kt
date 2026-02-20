package chat.stoat.composables.voice

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import chat.stoat.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoicePermissionSwitch(onCancel: () -> Unit, onPermissionGranted: @Composable () -> Unit) {
    val context = LocalContext.current

    var permissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        ),
        onPermissionsResult = {}
    )
    var fullyRevoked = permissionState
        .revokedPermissions
        .any { it.status is PermissionStatus.Denied && !it.status.shouldShowRationale }

    if (permissionState.allPermissionsGranted) {
        onPermissionGranted()
    } else if (permissionState.shouldShowRationale || fullyRevoked) {
        ModalBottomSheet(
            onDismissRequest = onCancel,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            sheetGesturesEnabled = false,
            dragHandle = {}
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.voice_join_permission_rationale_heading),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.voice_join_permission_rationale_description),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic_24dp),
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(R.string.voice_join_permission_rationale_permission_mic),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_videocam_24dp),
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(R.string.voice_join_permission_rationale_permission_camera),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = stringResource(R.string.voice_join_permission_rationale_assurance),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (fullyRevoked) {
                            // Launch settings to allow the user to manually enable permissions
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = "package:${context.packageName}".toUri()
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            context.startActivity(intent)
                        } else {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.voice_join_permission_rationale_cta))
                }
            }
        }
    } else {
        LaunchedEffect(Unit) {
            permissionState.launchMultiplePermissionRequest()
        }
    }
}