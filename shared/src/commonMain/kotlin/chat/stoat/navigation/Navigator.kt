package chat.stoat.navigation

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey

@Stable
class Navigator(private val backStack: MutableList<NavKey>) {
    fun navigate(key: NavKey) {
        backStack.add(key)
    }

    fun popBackStack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    // Adds before removing so the back stack is never empty
    fun resetTo(key: NavKey) {
        backStack.add(key)
        backStack.subList(0, backStack.lastIndex).clear()
    }
}
