package chat.stoat.core.designsystem.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun RemoteImage(
    url: String,
    description: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    width: Int = 0,
    height: Int = 0,
    allowAnimation: Boolean = true,
    blurred: Boolean = false
) {
    val density = LocalDensity.current
    PlatformRemoteImage(
        url = url,
        description = description,
        modifier = with(density) { modifier.width(width.toDp()).height(height.toDp()) }
            .then(if (blurred) Modifier.blur(BlurredImageRadius) else Modifier),
        contentScale = contentScale,
        allowAnimation = allowAnimation,
        blurred = blurred,
    )
}

private val BlurredImageRadius = 12.dp

@Composable
internal expect fun PlatformRemoteImage(
    url: String,
    description: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    allowAnimation: Boolean,
    blurred: Boolean,
)
