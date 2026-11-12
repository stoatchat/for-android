package chat.stoat.composables.skeletons

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable

@Composable
fun skeletonColourOnBackground() = LocalContentColor.current.copy(alpha = 0.4f)