package chat.revolt.composables.generic

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.settings.LoadedSettings
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@Composable
fun InlineMediaPicker(
    currentModel: Any?,
    modifier: Modifier = Modifier,
    mimeType: String = "image/*",
    circular: Boolean = false,
    useAvatarCircularity: Boolean = false,
    onPick: (Uri) -> Unit,
    canRemove: Boolean = true,
    onRemove: () -> Unit = {},
    enabled: Boolean = true
) {
    if (circular) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            InlineMediaPickerMediaPicker(
                currentModel = currentModel,
                mimeType = mimeType,
                circular = true,
                useAvatarCircularity = useAvatarCircularity,
                onPick = onPick
            )

            if (canRemove) {
                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        onRemove()
                    },
                    enabled = (currentModel != null) && enabled
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icn_close_24dp),
                        contentDescription = stringResource(R.string.inline_media_picker_remove)
                    )
                }
            }
        }
    } else {
        Column(modifier) {
            InlineMediaPickerMediaPicker(
                currentModel = currentModel,
                mimeType = mimeType,
                circular = false,
                onPick = onPick
            )

            if (canRemove) {
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        onRemove()
                    },
                    enabled = (currentModel != null) && enabled,
                    modifier = Modifier.width(480.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icn_close_24dp),
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(R.string.inline_media_picker_remove),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun InlineMediaPickerMediaPicker(
    currentModel: Any?,
    mimeType: String = "image/*",
    circular: Boolean = false,
    useAvatarCircularity: Boolean = false,
    enabled: Boolean = true,
    onPick: (Uri) -> Unit
) {
    val documentsUiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onPick(uri)
        }
    }

    if (currentModel != null) {
        GlideImage(
            model = currentModel,
            contentDescription = stringResource(R.string.inline_media_picker_current_description),
            contentScale = if (circular) ContentScale.Crop else ContentScale.FillWidth,
            modifier = if (circular) {
                Modifier
                    .then(
                        if (useAvatarCircularity) {
                            Modifier.clip(RoundedCornerShape(LoadedSettings.avatarRadius))
                        } else {
                            Modifier.clip(CircleShape)
                        }
                    )
                    .width(82.dp)
                    .height(82.dp)
            } else {
                Modifier
                    .clip(MaterialTheme.shapes.large)
                    .width(480.dp)
                    .height(140.dp)
            }.clickable {
                if (enabled) documentsUiLauncher.launch(mimeType)
            },
            transition = CrossFade,
        )
    } else {
        Box(
            modifier = if (circular) {
                Modifier
                    .then(
                        if (useAvatarCircularity) {
                            Modifier.clip(RoundedCornerShape(LoadedSettings.avatarRadius))
                        } else {
                            Modifier.clip(CircleShape)
                        }
                    )
                    .width(82.dp)
                    .height(82.dp)
            } else {
                Modifier
                    .clip(MaterialTheme.shapes.large)
                    .width(480.dp)
                    .height(140.dp)
            }
                .clickable {
                    if (enabled) documentsUiLauncher.launch(mimeType)
                }
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            if (circular) {
                Icon(
                    painter = painterResource(R.drawable.icn_add_24dp),
                    contentDescription = stringResource(R.string.inline_media_picker_no_media_placeholder)
                )
            } else {
                Text(
                    text = stringResource(R.string.inline_media_picker_no_media_placeholder),
                    style = MaterialTheme.typography.bodySmall.copy(
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}