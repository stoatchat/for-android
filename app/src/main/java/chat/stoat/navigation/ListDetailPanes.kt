package chat.stoat.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.window.core.layout.WindowSizeClass
import chat.stoat.composables.generic.ListDetailPlaceholder
import chat.stoat.core.designsystem.theme.NavTweenFloat
import chat.stoat.core.designsystem.theme.NavTweenInt
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

private const val PaneRoleKey = "chat.stoat.navigation.PaneRole"

@PublishedApi
internal sealed interface PaneRole {
    val group: Any

    data class List(override val group: Any) : PaneRole
    data class Detail(override val group: Any) : PaneRole
}

private val NavEntry<*>.paneRole: PaneRole? get() = metadata[PaneRoleKey] as? PaneRole

// Same format as Navigation 3's default content key, set explicitly so reported routes can be
// matched to entries
@PublishedApi
internal fun paneContentKey(key: Any): Any = "$key:${key::class}"

/** Registers one list screen and the detail screens it opens. */
class ListDetailGroupScope @PublishedApi internal constructor(
    @PublishedApi internal val scope: EntryProviderScope<NavKey>,
    @PublishedApi internal val group: Any,
    @PublishedApi internal val navigator: Navigator,
) {
    inline fun <reified K : NavKey> list(
        metadata: Map<String, Any> = emptyMap(),
        noinline content: @Composable (key: K, nav: Navigator) -> Unit,
    ) = scope.entry<K>(
        clazzContentKey = ::paneContentKey,
        metadata = metadata + paneRole(PaneRole.List(group)),
    ) { key ->
        content(key, navigator.rememberScopedTo(key))
    }

    inline fun <reified K : NavKey> detail(
        metadata: Map<String, Any> = emptyMap(),
        noinline content: @Composable (key: K, nav: Navigator) -> Unit,
    ) = scope.entry<K>(
        clazzContentKey = ::paneContentKey,
        metadata = metadata + paneRole(PaneRole.Detail(group)),
    ) { key ->
        content(key, navigator.rememberScopedTo(key))
    }

    @PublishedApi
    internal fun paneRole(role: PaneRole): Pair<String, Any> = PaneRoleKey to role
}

fun EntryProviderScope<NavKey>.listDetail(
    group: Any,
    navigator: Navigator,
    block: ListDetailGroupScope.() -> Unit,
) = ListDetailGroupScope(this, group, navigator).block()

/** How the two panes are split, or null when the window only fits one pane. */
@Immutable
private data class PaneSplit(val listWidth: Dp, val hinge: Rect?)

/**
 * Shows a list and its latest detail side by side when the window allows it: on expanded and
 * larger windows that aren't compact in height, or at any size when a separating vertical hinge
 * (book posture, dual screens) divides the window, in which case the panes are split exactly at
 * the hinge.
 *
 * Details opened automatically from the topmost [listDetailItem] row are dropped again when the window goes back to a single
 * pane while they are still on top, so the list shows instead of a page the user never picked
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberListDetailSceneStrategy(backStack: MutableList<NavKey>): SceneStrategy<NavKey> {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = calculatePaneScaffoldDirective(adaptiveInfo)
    val hinge = directive.excludedBounds.firstOrNull()
    // Compact-height windows (phones in landscape) stay single-pane even when wide enough
    val tallEnough = adaptiveInfo.windowSizeClass
        .isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val split = when {
        hinge != null -> PaneSplit(directive.defaultPanePreferredWidth, hinge)
        directive.maxHorizontalPartitions >= 2 && tallEnough ->
            PaneSplit(directive.defaultPanePreferredWidth, null)

        else -> null
    }

    var autoOpened by remember { mutableStateOf<NavKey?>(null) }
    val top = backStack.lastOrNull()
    LaunchedEffect(top) {
        if (top != autoOpened) autoOpened = null
    }
    LaunchedEffect(split == null) {
        if (split == null && autoOpened != null && backStack.lastOrNull() == autoOpened) {
            backStack.removeAt(backStack.lastIndex)
        }
        autoOpened = null
    }

    return remember(split, backStack) {
        ListDetailSceneStrategy(split) { key ->
            autoOpened = key
            backStack.add(key)
        }
    }
}

private class ListDetailSceneStrategy(
    private val split: PaneSplit?,
    private val openDefault: (NavKey) -> Unit,
) : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(
        entries: List<NavEntry<NavKey>>,
    ): Scene<NavKey>? {
        split ?: return null
        if (entries.last().paneRole == null) return null

        // A list opened from a detail pane (e.g. channel settings from server settings) becomes
        // another detail layer, so the outer list stays visible and at most two panes show
        var listIndex = -1
        for (index in entries.indices.reversed()) {
            val role = entries[index].paneRole ?: break
            if (role is PaneRole.List && entries.getOrNull(index - 1)?.paneRole !is PaneRole.Detail) {
                listIndex = index
                break
            }
        }
        if (listIndex < 0) return null

        val list = entries[listIndex]
        val details = entries.subList(listIndex + 1, entries.size)
        return ListDetailScene(
            key = list.paneRole!!.group,
            list = list,
            detail = details.lastOrNull(),
            depth = details.size,
            split = split,
            openDefault = openDefault,
            previousEntries = entries.dropLast(1),
        )
    }
}

private data class DetailState(val entry: NavEntry<NavKey>?, val depth: Int, val order: Int)

private data class ListDetailScene(
    override val key: Any,
    val list: NavEntry<NavKey>,
    val detail: NavEntry<NavKey>?,
    val depth: Int,
    val split: PaneSplit,
    val openDefault: (NavKey) -> Unit,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> {
    override val entries: List<NavEntry<NavKey>> = listOfNotNull(list, detail)

    override val metadata: Map<String, Any> get() = list.metadata

    override val content: @Composable () -> Unit = {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        var originX by remember { mutableFloatStateOf(0f) }
        var widthPx by remember { mutableFloatStateOf(0f) }

        val items = remember(list.contentKey) { ListPaneItems() }

        // Each list fills its empty detail pane with its topmost row at most once, so backing out
        // of that detail leaves the pane empty instead of reopening it
        val defaultOpenedFor = remember { mutableStateSetOf<Any>() }
        val awaitingDefault = detail == null && list.contentKey !in defaultOpenedFor
        LaunchedEffect(list.contentKey, awaitingDefault) {
            if (!awaitingDefault) return@LaunchedEffect
            val first = snapshotFlow { items.ordered.firstOrNull() }.filterNotNull().first()
            defaultOpenedFor += list.contentKey
            openDefault(first)
        }

        // Rows register during the list's first layout; until then the pane stays blank instead
        // of flashing the placeholder
        var rowsSettled by remember(list.contentKey) { mutableStateOf(false) }
        LaunchedEffect(list.contentKey) {
            withFrameNanos {}
            withFrameNanos {}
            rowsSettled = true
        }

        val hinge = split.hinge
        val (listWidth, gap) = if (hinge != null && widthPx > 0f) {
            val leftPart = (hinge.left - originX).coerceIn(0f, widthPx)
            val rightPart = (widthPx - (hinge.right - originX)).coerceIn(0f, widthPx)
            val listPart = if (layoutDirection == LayoutDirection.Ltr) leftPart else rightPart
            with(density) { listPart.toDp() to hinge.width.toDp() }
        } else {
            split.listWidth to null
        }

        Row(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    originX = it.positionInWindow().x
                    widthPx = it.size.width.toFloat()
                }
        ) {
            Box(
                Modifier
                    .width(listWidth)
                    .fillMaxHeight()
            ) {
                CompositionLocalProvider(LocalListPaneItems provides items) {
                    list.Content()
                }
            }
            if (gap != null) {
                Spacer(Modifier.width(gap))
            } else {
                VerticalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))
            }

            val order = detail?.let { entry ->
                items.ordered.indexOfFirst { paneContentKey(it) == entry.contentKey }
            } ?: -1
            AnimatedContent(
                targetState = DetailState(detail, depth, order),
                contentKey = { it.entry?.contentKey },
                transitionSpec = { detailTransition(layoutDirection) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds(),
                label = "ListDetailScene",
            ) { state ->
                // Deeper layers keep their back arrow, since it leads somewhere the list doesn't
                CompositionLocalProvider(LocalDetailBesideList provides (state.depth == 1)) {
                    when {
                        state.entry != null -> state.entry.Content()
                        awaitingDefault && !rowsSettled -> Surface(Modifier.fillMaxSize()) {}
                        else -> ListDetailPlaceholder()
                    }
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<DetailState>.detailTransition(
    layoutDirection: LayoutDirection,
): ContentTransform {
    val from = initialState
    val to = targetState
    val forward = if (layoutDirection == LayoutDirection.Ltr) 1 else -1

    return when {
        from.entry == null || to.entry == null -> fadeIn(NavTweenFloat) togetherWith fadeOut(
            NavTweenFloat
        )

        to.depth > from.depth ->
            slideInHorizontally(NavTweenInt) { forward * it / 3 } + fadeIn(NavTweenFloat) togetherWith
                    slideOutHorizontally(NavTweenInt) { -forward * it / 3 } + fadeOut(NavTweenFloat)

        to.depth < from.depth ->
            slideInHorizontally(NavTweenInt) { -forward * it / 3 } + fadeIn(NavTweenFloat) togetherWith
                    slideOutHorizontally(NavTweenInt) { forward * it / 2 } + fadeOut(NavTweenFloat)

        to.depth == 1 && from.order >= 0 && to.order >= 0 && to.order != from.order -> {
            val down = if (to.order > from.order) 1 else -1
            slideInVertically(NavTweenInt) { down * it / 3 } + fadeIn(NavTweenFloat) togetherWith
                    slideOutVertically(NavTweenInt) { -down * it / 3 } + fadeOut(NavTweenFloat)
        }

        else -> fadeIn(NavTweenFloat) togetherWith fadeOut(NavTweenFloat)
    }
}
