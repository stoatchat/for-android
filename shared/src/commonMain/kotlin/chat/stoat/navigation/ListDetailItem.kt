package chat.stoat.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.navigation3.runtime.NavKey

/** Detail routes tagged with [listDetailItem] inside a list pane, in the order they are drawn. */
@Stable
class ListPaneItems {
    private val placed = HashMap<Any, Pair<NavKey, Offset>>()

    var ordered: List<NavKey> by mutableStateOf(emptyList())
        private set

    internal fun place(owner: Any, route: NavKey, position: Offset) {
        placed[owner] = route to position
        publish()
    }

    internal fun remove(owner: Any) {
        if (placed.remove(owner) != null) publish()
    }

    private fun publish() {
        val next = placed.values
            .sortedWith(compareBy({ it.second.y }, { it.second.x }))
            .map { it.first }
            .distinct()
        if (next != ordered) ordered = next
    }
}

val LocalListPaneItems = staticCompositionLocalOf<ListPaneItems?> { null }

/**
 * Marks this row as opening [route] in the detail pane. On two-pane layouts the rows' drawn order
 * decides whether switching between them slides up or down, and the topmost row fills an empty
 * detail pane. Does nothing on single-pane layouts.
 */
fun Modifier.listDetailItem(route: NavKey): Modifier = this then ListDetailItemElement(route)

private data class ListDetailItemElement(val route: NavKey) :
    ModifierNodeElement<ListDetailItemNode>() {
    override fun create() = ListDetailItemNode(route)

    override fun update(node: ListDetailItemNode) = node.updateRoute(route)
}

private class ListDetailItemNode(private var route: NavKey) :
    Modifier.Node(),
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    private var items: ListPaneItems? = null
    private var lastPosition: Offset? = null

    override fun onDetach() {
        items?.remove(this)
        items = null
        lastPosition = null
    }

    // The registry is re-read on every placement because Navigation 3 moves entry content between
    // scenes (single pane / two pane) without detaching it
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val current = currentValueOf(LocalListPaneItems)
        if (current !== items) {
            items?.remove(this)
            items = current
        }
        val position = coordinates.positionInWindow()
        lastPosition = position
        items?.place(this, route, position)
    }

    fun updateRoute(route: NavKey) {
        this.route = route
        lastPosition?.let { items?.place(this, route, it) }
    }
}
