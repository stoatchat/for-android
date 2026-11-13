package chat.stoat.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal actual fun PlatformRemoteImage(
    url: String,
    description: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    allowAnimation: Boolean,
    blurred: Boolean,
) {
    GlideImage(
        model = url,
        contentDescription = description,
        contentScale = contentScale,
        modifier = modifier,
        transition = CrossFade,
        requestBuilderTransform = { rb ->
            when {
                blurred -> rb.override(SPOILER_DECODE_SIZE_PX).transform(SpoilerBlurTransformation).dontAnimate()
                !allowAnimation -> rb.dontAnimate()
                else -> rb
            }
        }
    )
}

private const val SPOILER_DECODE_SIZE_PX = 128
