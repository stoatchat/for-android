package chat.stoat.screens.chat.standalone

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.core.model.schemas.ChannelUnread
import logcat.LogPriority
import logcat.logcat

sealed class CatchUpCard {
    data class UnreadMessageInChannel(
        val channelId: String,
        val lastReadMessageId: String,
        val newestMessageId: String
    ) : CatchUpCard()
}

class CatchUpScreenViewModel : ViewModel() {
    private val deck = ArrayDeque<CatchUpCard>(initialCapacity = 3)
    private var dataSource: Iterator<ChannelUnread> = emptyList<ChannelUnread>().iterator()
    var initComplete = false
        private set

    private val _cards = mutableStateOf<List<CatchUpCard>>(emptyList())
    val cards: State<List<CatchUpCard>> = _cards

    fun initWith(unreads: List<ChannelUnread>) {
        dataSource = unreads.iterator()

        repeat(3) {
            dealNewCard()
        }

        initComplete = true
    }

    fun dealNewCard() {
        if (dataSource.hasNext()) {
            val unread = dataSource.next()
            unread.last_id?.let { lastId ->
                deck.addLast(
                    CatchUpCard.UnreadMessageInChannel(
                        channelId = unread.id,
                        lastReadMessageId = lastId,
                        newestMessageId = lastId // TODO: Replace with actual newest message ID
                    )
                )
                deckUpdated()
            }
        } else {
            logcat(LogPriority.WARN) { "No more unreads to deal!" }
        }
    }

    fun swipedCard() {
        if (deck.isNotEmpty()) {
            deck.removeFirst()
            deckUpdated()
        }
        dealNewCard()
    }

    fun deckUpdated() {
        _cards.value = deck.toList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchUpScreen(navController: NavController, viewModel: CatchUpScreenViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        viewModel.initWith(StoatAPI.unreads.getAllUnreads())
    }

    val primaryContainer = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    val colourKeep = remember { Color(0xFFF84848).copy(alpha = 0.5f) }
    val colourRead = remember { Color(0xFF3ABF7E).copy(alpha = 0.5f) }

    var currentColour by remember { mutableStateOf(primaryContainer) }
    val gradientColour by animateColorAsState(
        targetValue = currentColour,
        animationSpec = tween(durationMillis = 500)
    )

    val cards by viewModel.cards

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.catch_up),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }
    ) { pv ->
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .background(
                        Brush.linearGradient(
                            0.2f to MaterialTheme.colorScheme.background,
                            1.0f to gradientColour,
                            end = Offset.Infinite.copy(x = 0f)
                        )
                    )
                    .fillMaxSize()
            )

            Box(
                Modifier
                    .padding(pv)
                    .imePadding()
            ) {
                for (card in cards.reversed()) {
                    when (card) {
                        is CatchUpCard.UnreadMessageInChannel -> {
                            val state = rememberSwipeToDismissBoxState()

                            LaunchedEffect(state.currentValue) {
                                if (state.currentValue == SwipeToDismissBoxValue.StartToEnd) {
                                    // Start to end is mark read
                                    viewModel.swipedCard()
                                    state.reset()
                                } else if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                    // End to start is skip
                                    viewModel.swipedCard()
                                    state.reset()
                                }
                            }

                            LaunchedEffect(state.dismissDirection) {
                                when (state.dismissDirection) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        currentColour = colourRead
                                    }

                                    SwipeToDismissBoxValue.EndToStart -> {
                                        currentColour = colourKeep
                                    }

                                    SwipeToDismissBoxValue.Settled -> {
                                        currentColour = primaryContainer
                                    }
                                }
                            }

                            SwipeToDismissBox(
                                state = state,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxSize(),
                                backgroundContent = {
                                    if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                        Text(
                                            "Mark as read",
                                            Modifier
                                                .fillMaxWidth()
                                                .background(Color.Green)
                                        )
                                    } else if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                        Text(
                                            "Keep unread",
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Red)
                                        )
                                    }
                                }
                            ) {
                                Column(Modifier.background(Color.Black)) {
                                    Text("Channel ID: ${card.channelId}")
                                    Text("Last Read Message ID: ${card.lastReadMessageId}")
                                    Text("Newest Message ID: ${card.newestMessageId}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}