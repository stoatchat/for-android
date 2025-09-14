package chat.zekochat.internals.extensions

import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TransparentListItemColours: ListItemColors
    @Composable
    get() = ListItemDefaults.colors().copy(
        containerColor = Color.Transparent
    )