package chat.stoat.navigation

import androidx.compose.runtime.compositionLocalOf

/** True while a screen is the first detail layer next to its list, where a back arrow is redundant
 * since theres nothing above in the back stack */
val LocalDetailBesideList = compositionLocalOf { false }
