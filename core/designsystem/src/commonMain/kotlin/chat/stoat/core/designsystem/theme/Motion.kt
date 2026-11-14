package chat.stoat.core.designsystem.theme

import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

val StoatTweenInt: FiniteAnimationSpec<IntOffset> = tween(400, easing = EaseInOutExpo)
val StoatTweenSize: FiniteAnimationSpec<IntSize> = tween(400, easing = EaseInOutExpo)
val StoatTweenFloat: FiniteAnimationSpec<Float> = tween(400, easing = EaseInOutExpo)
val StoatTweenDp: FiniteAnimationSpec<Dp> = tween(400, easing = EaseInOutExpo)
val StoatTweenColour: FiniteAnimationSpec<Color> = tween(400, easing = EaseInOutExpo)

val NavTweenInt: FiniteAnimationSpec<IntOffset> = tween(350, easing = EaseInOutExpo)
val NavTweenFloat: FiniteAnimationSpec<Float> = tween(350, easing = EaseInOutExpo)
