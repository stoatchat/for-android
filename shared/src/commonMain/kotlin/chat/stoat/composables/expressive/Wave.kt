package chat.stoat.composables.expressive

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun Wave(
    colour: Brush,
    modifier: Modifier = Modifier,
    stroke: Stroke,
    amplitude: Float,
    wavelength: Dp,
    waveSpeed: Dp = 0.dp
) {
    val movingWaveOffset =
        if (waveSpeed > 0.dp && wavelength > 0.dp) {
            val durationMillis =
                ((wavelength / waveSpeed) * 1000f).roundToInt().coerceAtLeast(50)
            rememberInfiniteTransition(label = "wave transition").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "wave offset"
            )
        } else {
            null
        }

    Spacer(
        modifier = modifier
            .clipToBounds()
            .drawWithCache {
                val wavelengthPx = wavelength.toPx()
                val waveAmplitude = amplitude.coerceIn(0f, 1f)
                val path = Path().apply {
                    val centerY = size.height / 2f
                    moveTo(0f, centerY)

                    if (waveAmplitude == 0f || wavelengthPx <= 0f) {
                        lineTo(size.width, centerY)
                    } else {
                        val halfWavelength = wavelengthPx / 2f
                        var anchorX = halfWavelength
                        var controlX = halfWavelength / 2f
                        var controlY =
                            centerY + (size.height - stroke.width).coerceAtLeast(0f) * waveAmplitude
                        val widthWithExtraPhase = size.width + wavelengthPx * 2f

                        while (anchorX <= widthWithExtraPhase) {
                            quadraticTo(controlX, controlY, anchorX, centerY)
                            anchorX += halfWavelength
                            controlX += halfWavelength
                            controlY = centerY * 2f - controlY
                        }
                    }
                }

                if (movingWaveOffset == null) {
                    onDrawBehind {
                        drawPath(
                            path = path,
                            brush = colour,
                            style = stroke
                        )
                    }
                } else {
                    val shiftedPath = Path()
                    onDrawBehind {
                        shiftedPath.rewind()
                        shiftedPath.addPath(
                            path = path,
                            offset = Offset(
                                x = -movingWaveOffset.value * wavelengthPx,
                                y = 0f
                            )
                        )

                        drawPath(
                            path = shiftedPath,
                            brush = colour,
                            style = stroke
                        )
                    }
                }
            }
    )
}
