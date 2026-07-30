package chat.stoat.screens.chat.views.channel

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChannelHistoryScrollTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun historicalPagesPreservePositionUntilTheListReachesPresentDay() {
        val messages = mutableStateListOf<String>().apply {
            addAll((0 until 100).map { "old-$it" })
        }
        var showBottomAnchor by mutableStateOf(false)
        lateinit var state: LazyListState

        composeRule.setContent {
            state = rememberLazyListState()
            LazyColumn(
                modifier = Modifier
                    .width(200.dp)
                    .height(240.dp),
                state = state,
                reverseLayout = true,
            ) {
                if (showBottomAnchor) {
                    item(key = "guaranteed_first") {
                        Spacer(Modifier.height(1.dp))
                    }
                }
                items(messages, key = { it }) { message ->
                    Text(
                        text = message,
                        modifier = Modifier.height(48.dp),
                    )
                }
            }
        }

        repeat(2) { page ->
            composeRule.runOnIdle {
                runBlocking {
                    state.scrollToItem(index = 3, scrollOffset = 11)
                }
            }
            val anchor = composeRule.runOnIdle { state.currentAnchor() }

            composeRule.runOnIdle {
                messages.addAll(
                    index = 0,
                    elements = (0 until 50).map { "newer-$page-$it" },
                )
            }

            composeRule.runOnIdle {
                assertEquals(anchor, state.currentAnchor())
                assertTrue(state.firstVisibleItemIndex > 6)
            }
        }

        composeRule.runOnIdle {
            runBlocking {
                state.scrollToItem(index = 3, scrollOffset = 11)
            }
        }
        val finalHistoricalAnchor = composeRule.runOnIdle { state.currentAnchor() }
        composeRule.runOnIdle {
            messages.addAll(
                index = 0,
                elements = (0 until 50).map { "final-$it" },
            )
            showBottomAnchor = true
        }
        composeRule.runOnIdle {
            assertEquals(finalHistoricalAnchor, state.currentAnchor())
            assertTrue(state.firstVisibleItemIndex > 6)
        }

        composeRule.runOnIdle {
            runBlocking {
                state.scrollToItem(0)
            }
        }
        composeRule.runOnIdle {
            messages.add(0, "live-message")
        }
        composeRule.runOnIdle {
            assertTrue(
                state.layoutInfo.visibleItemsInfo.any { it.key == "live-message" }
            )
        }
    }

    @Test
    fun reversedListUsesViewportDisplacementToCenterAnItem() {
        val messages = (0 until 100).map { "message-$it" }
        lateinit var state: LazyListState

        composeRule.setContent {
            state = rememberLazyListState()
            LazyColumn(
                modifier = Modifier
                    .width(200.dp)
                    .height(240.dp),
                state = state,
                reverseLayout = true,
            ) {
                items(messages, key = { it }) { message ->
                    Text(
                        text = message,
                        modifier = Modifier.height(48.dp),
                    )
                }
            }
        }

        composeRule.runOnIdle {
            runBlocking {
                state.scrollToItem(20)
            }
        }
        composeRule.runOnIdle {
            val viewportCenter =
                (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2
            val targetCenter = state.itemCenter("message-20")
            runBlocking {
                state.scrollBy((targetCenter - viewportCenter).toFloat())
            }
        }
        composeRule.runOnIdle {
            val viewportCenter =
                (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2
            assertEquals(viewportCenter, state.itemCenter("message-20"))
        }
    }

    @Test
    fun reversedListCanCenterAnOlderItemFromTheTopEdge() {
        val messages = (0 until 100).map { "message-$it" }
        lateinit var state: LazyListState

        composeRule.setContent {
            state = rememberLazyListState()
            LazyColumn(
                modifier = Modifier
                    .width(200.dp)
                    .height(240.dp),
                state = state,
                reverseLayout = true,
            ) {
                items(messages, key = { it }) { message ->
                    Text(
                        text = message,
                        modifier = Modifier.height(48.dp),
                    )
                }
            }
        }

        composeRule.runOnIdle {
            runBlocking {
                state.scrollToItem(20)
            }
        }
        composeRule.runOnIdle {
            runBlocking {
                state.scrollToItem(40)
            }

            val viewportCenter =
                (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2
            val targetCenter = state.itemCenter("message-40")
            runBlocking {
                state.scrollBy((targetCenter - viewportCenter) * 2f)
            }
        }
        composeRule.runOnIdle {
            val viewportCenter =
                (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2
            assertTrue(state.itemCenter("message-40") > viewportCenter)

            runBlocking {
                state.scrollBy(
                    (state.itemCenter("message-40") - viewportCenter).toFloat()
                )
            }
        }
        composeRule.runOnIdle {
            val viewportCenter =
                (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2
            assertEquals(viewportCenter, state.itemCenter("message-40"))
        }
    }

    private fun LazyListState.currentAnchor(): Pair<Any, Int> {
        val firstVisibleItem = layoutInfo.visibleItemsInfo
            .first { it.index == firstVisibleItemIndex }
        return firstVisibleItem.key to firstVisibleItem.offset
    }

    private fun LazyListState.itemCenter(key: Any): Int {
        val item = layoutInfo.visibleItemsInfo.first { it.key == key }
        return item.offset + item.size / 2
    }
}
