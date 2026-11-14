package chat.stoat.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey

@Stable
class Navigator(
    private val backStack: MutableList<NavKey>,
    /**
     * With an [anchor], operations act relative to that entry, instead of the top of the back stack,
     * so a screen shown in a list pane next to a detail pane still navigates as if it's on top.
     */
    private val anchor: NavKey? = null,
) {
    fun navigate(key: NavKey) {
        anchorIndex()?.let { clearAbove(it) }
        backStack.add(key)
    }

    fun popBackStack() {
        if (anchor == null) {
            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            return
        }
        val index = anchorIndex() ?: return
        if (index > 0) backStack.subList(index, backStack.size).clear()
    }

    // Adds before removing so the back stack is never empty
    fun resetTo(key: NavKey) {
        backStack.add(key)
        backStack.subList(0, backStack.lastIndex).clear()
    }

    fun scopedTo(key: NavKey): Navigator = Navigator(backStack, key)

    private fun anchorIndex(): Int? =
        anchor?.let { backStack.lastIndexOf(it) }?.takeIf { it >= 0 }

    private fun clearAbove(index: Int) {
        if (index < backStack.lastIndex) backStack.subList(index + 1, backStack.size).clear()
    }
}

@Composable
fun Navigator.rememberScopedTo(key: NavKey): Navigator = remember(this, key) { scopedTo(key) }
