package chat.stoat.activities.voice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.composables.generic.Presence
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.generic.presenceColour
import chat.stoat.ui.theme.StoatTheme
import chat.stoat.ui.theme.Theme
import chat.stoat.ui.theme.getDefaultFont
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class IncomingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            IncomingCall()
        }
    }
}

@Composable
fun IncomingCall() {
    StoatTheme(
        requestedTheme = if (isSystemInDarkTheme()) Theme.Default else Theme.Light,
        requestedUserInterfaceFont = getDefaultFont()
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            IncomingCallInner()
        }
    }
}

private enum class CallSwiperState {
    Initial,
    Accept,
    Decline
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IncomingCallInner() {
    val bgInfinite = rememberInfiniteTransition(label = "Background")
    val bgColour by bgInfinite.animateColor(
        initialValue = MaterialTheme.colorScheme.surfaceContainerHighest,
        targetValue = MaterialTheme.colorScheme.surfaceContainerLowest,
        label = "Background Colour",
        animationSpec = infiniteRepeatable(
            animation = tween(1000, 0, FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    var swiperLabelColourLerp by remember { mutableFloatStateOf(0f) }
    val swiperLabelColourLerpAnim by animateFloatAsState(
        targetValue = swiperLabelColourLerp,
        animationSpec = tween(250),
        label = "Swiper Label Colour Lerp state"
    )
    LaunchedEffect(Unit) {
        while (true) {
            swiperLabelColourLerp = 0f
            delay(1000)
            swiperLabelColourLerp = 1f
            delay(4000)
        }
    }

    val swiperState = remember { AnchoredDraggableState(CallSwiperState.Initial) }
    val swiperWidth = 330.dp
    val swiperWidthThird = swiperWidth / 3
    val density = LocalDensity.current
    val swiperWidthPx = with(density) { swiperWidth.toPx() }
    val swiperThirdPx = with(density) { swiperWidthThird.toPx() }
    SideEffect {
        swiperState.updateAnchors(
            DraggableAnchors {
                CallSwiperState.Decline at 35f
                CallSwiperState.Initial at (swiperWidthPx / 2f) - (swiperThirdPx / 2f)
                CallSwiperState.Accept at swiperWidthPx - swiperThirdPx - 35f
            }
        )
    }

    LaunchedEffect(swiperState.currentValue) {
        when (swiperState.currentValue) {
            CallSwiperState.Accept -> {
                swiperState.animateTo(CallSwiperState.Initial)
            }

            CallSwiperState.Decline -> {
                swiperState.animateTo(CallSwiperState.Initial)
            }

            else -> {}
        }
    }

    val swiperRotation = remember(swiperState.offset) {
        if (swiperState.offset.isNaN()) 0f
        else {
            val declineAnchor = 35f
            val initialAnchor = (swiperWidthPx / 2f) - (swiperThirdPx / 2f)
            val acceptAnchor = swiperWidthPx - swiperThirdPx - 35f

            when {
                swiperState.offset <= initialAnchor -> {
                    // Moving towards decline: interpolate from 0° to -90°
                    val progress =
                        (initialAnchor - swiperState.offset) / (initialAnchor - declineAnchor)
                    -225f * progress.coerceIn(0f, 1f)
                }

                swiperState.offset >= initialAnchor -> {
                    // Moving towards accept: interpolate from 0° to 90°
                    val progress =
                        (swiperState.offset - initialAnchor) / (acceptAnchor - initialAnchor)
                    45f * progress.coerceIn(0f, 1f)
                }

                else -> 0f // At initial position
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(bgColour),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Incoming Call on Stoat",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 24.sp
            )
            Text(
                "cat",
                style = MaterialTheme.typography.displayLargeEmphasized,
                fontWeight = FontWeight.SemiBold
            )
            with(density) {
                RemoteImage(
                    url = "https://cdn.revoltusercontent.com/attachments/K1CDpnvORz2fzUhgq47mcL7N4gccWGqNYYeGaJVvyp/image.png",
                    description = null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .fillMaxWidth(0.5f)
                        .aspectRatio(1f),
                )
            }
            Spacer(Modifier.fillMaxHeight(.33f))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .height(84.dp)
                    .width(swiperWidth)
                    .then(
                        if (isSystemInDarkTheme()) Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        presenceColour(Presence.Dnd).copy(alpha = 0.05f),
                                        presenceColour(Presence.Online).copy(alpha = 0.05f),
                                    )
                                )
                            ) else Modifier.background(MaterialTheme.colorScheme.surfaceBright)
                    ),
            ) {
                Text(
                    "Decline",
                    color = lerp(
                        presenceColour(Presence.Dnd),
                        MaterialTheme.colorScheme.onSurface,
                        swiperLabelColourLerpAnim
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 30.dp)
                )
                Text(
                    "Accept",
                    color = lerp(
                        presenceColour(Presence.Online),
                        MaterialTheme.colorScheme.onSurface,
                        swiperLabelColourLerpAnim
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 30.dp)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(swiperWidthThird)
                        .height(64.dp)
                        .offset {
                            IntOffset(
                                x = swiperState.requireOffset().roundToInt(),
                                y = with(density) { (84.dp - 64.dp).toPx() / 2 }.roundToInt()
                            )
                        }
                        .anchoredDraggable(
                            swiperState,
                            Orientation.Horizontal,
                            flingBehavior =
                                AnchoredDraggableDefaults.flingBehavior(
                                    swiperState,
                                    positionalThreshold = { distance -> distance * 0.25f },
                                ),
                        )
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_call_24dp__fill),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .rotate(swiperRotation)
                    )
                }
            }
        }
    }
}