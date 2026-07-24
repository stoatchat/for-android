package chat.stoat.composables.screens.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TypingIndicatorAnimationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typingIndicatorMovesContentAboveItSmoothly() {
        var typingUsers by mutableStateOf(emptyList<String>())

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(200.dp)
                ) {
                    Column(Modifier.align(Alignment.BottomCenter)) {
                        Spacer(
                            Modifier
                                .testTag("content-above-typing-indicator")
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                        TypingIndicator(
                            users = typingUsers,
                            serverId = null,
                        )
                    }
                }
            }
        }

        val hiddenPosition = contentTop()

        composeRule.runOnIdle {
            typingUsers = listOf("typing-user")
        }
        composeRule.mainClock.advanceTimeBy(200)
        val enteringPosition = contentTop()
        composeRule.mainClock.advanceTimeBy(300)
        val shownPosition = contentTop()

        assertTrue(enteringPosition < hiddenPosition)
        assertTrue(enteringPosition > shownPosition)

        composeRule.runOnIdle {
            typingUsers = emptyList()
        }
        composeRule.mainClock.advanceTimeBy(200)
        val exitingPosition = contentTop()
        composeRule.mainClock.advanceTimeBy(300)
        val hiddenAgainPosition = contentTop()

        assertTrue(exitingPosition > shownPosition)
        assertTrue(exitingPosition < hiddenAgainPosition)
    }

    private fun contentTop(): Float =
        composeRule
            .onNodeWithTag("content-above-typing-indicator")
            .fetchSemanticsNode()
            .boundsInRoot
            .top
}
