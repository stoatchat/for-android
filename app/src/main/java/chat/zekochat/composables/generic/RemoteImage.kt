package chat.zekochat.composables.generic

import android.util.DisplayMetrics
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun RemoteImage(
    url: String,
    description: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    width: Int = 0,
    height: Int = 0,
    allowAnimation: Boolean = true
) {
    val context = LocalContext.current

    fun pxAsDp(px: Int): Dp {
        return (
                px / (
                        context.resources
                            .displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
                        )
                ).dp
    }

    GlideImage(
        model = url,
        contentDescription = description,
        contentScale = contentScale,
        modifier = modifier
            .width(pxAsDp(width))
            .height(pxAsDp(height)),
        transition = CrossFade,
        requestBuilderTransform = { rb ->
            if (!allowAnimation) rb.dontAnimate() else rb
        }
    )
}
