package chat.revolt.composables.screens.chat

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.routes.microservices.autumn.FileArgs
import chat.revolt.composables.generic.RemoteImage
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FilePreviewSheet(
    args: FileArgs,
    canRemove: Boolean,
    onRemove: () -> Unit,
    onToggleSpoiler: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var localIsSpoiler by remember { mutableStateOf(args.spoiler) }

    Column(
        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (args.contentType.startsWith("image/") || args.contentType.startsWith("video/")) {
            RemoteImage(
                url = args.file.toURI().toURL().toString(),
                contentScale = ContentScale.Fit,
                description = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }
        Text(
            args.filename, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
        )
        Text(
            Formatter.formatFileSize(context, args.file.length()),
            color = LocalContentColor.current.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                    onToggleSpoiler()
                    localIsSpoiler = !localIsSpoiler
                }
                .padding(top = 8.dp)
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(R.string.attachment_preview_spoiler)
                    )
                },
                supportingContent = {
                    Text(
                        stringResource(R.string.attachment_preview_spoiler_description)
                    )
                },
                trailingContent = {
                    Switch(
                        checked = localIsSpoiler,
                        onCheckedChange = null,
                    )
                },
                colors = ListItemDefaults.colors().copy(
                    containerColor = Color.Transparent,
                )
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onDismiss()
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.attachment_preview_close))
            }
            if (canRemove) {
                TextButton(onClick = {
                    onRemove()
                }, modifier = Modifier.weight(1f)) {
                    Icon(
                        painterResource(R.drawable.icn_attach_file_off_24dp),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.attachment_preview_remove))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentManager(
    attachments: List<FileArgs>,
    uploading: Boolean,
    uploadProgress: Float = 0f,
    onRemove: (FileArgs) -> Unit,
    onToggleSpoiler: (FileArgs) -> Unit,
    canRemove: Boolean = true,
    canPreview: Boolean = true
) {
    var showPreviewSheet by remember { mutableStateOf(false) }
    var previewingAttachment by remember { mutableStateOf<FileArgs?>(null) }
    val scope = rememberCoroutineScope()

    if (showPreviewSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showPreviewSheet = false
            }, sheetState = sheetState
        ) {
            previewingAttachment?.let {
                FilePreviewSheet(
                    args = it,
                    canRemove = canRemove,
                    onRemove = {
                        onRemove(it)
                        scope.launch {
                            sheetState.hide()
                            showPreviewSheet = false
                        }
                    },
                    onToggleSpoiler = {
                        onToggleSpoiler(it)
                    },
                    onDismiss = {
                        scope.launch {
                            sheetState.hide()
                            showPreviewSheet = false
                        }
                    }
                )
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = uploadProgress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "Upload progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            attachments.forEach { attachment ->
                Row(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            if (canPreview) {
                                previewingAttachment = attachment
                                showPreviewSheet = true
                            }
                        }
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(8.dp)) {
                    Text(attachment.filename, maxLines = 1)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        AnimatedVisibility(visible = uploading) {
            LinearProgressIndicator(
                progress = { animatedProgress }, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
fun AttachmentManagerPreview() {
    AttachmentManager(
        attachments = listOf(
            FileArgs(
                filename = "file1.png", contentType = "image/png", file = File("file1.png")
            ), FileArgs(
                filename = "file2.png", contentType = "image/png", file = File("file2.png")
            ), FileArgs(
                filename = "file3.png", contentType = "image/png", file = File("file3.png")
            )
        ), uploading = false, onToggleSpoiler = {}, onRemove = {})
}
