package chat.stoat.screens.chat

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.channel.searchChannel
import chat.stoat.composables.chat.SystemMessage
import chat.stoat.core.model.schemas.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_QUERY_LENGTH = 64
private const val SEARCH_DEBOUNCE_MS = 350L

enum class SearchSort(val apiValue: String, val label: Int) {
    RELEVANCE("Relevance", R.string.channel_search_sort_relevance),
    LATEST("Latest", R.string.channel_search_sort_latest),
    OLDEST("Oldest", R.string.channel_search_sort_oldest)
}

class ChannelSearchScreenViewModel : ViewModel() {
    val results = mutableStateListOf<Message>()
    var query by mutableStateOf("")
        private set
    var sort by mutableStateOf(SearchSort.RELEVANCE)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var hasSearched by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var searchJob: Job? = null

    fun onQueryChange(channelId: String, newQuery: String) {
        query = newQuery.take(MAX_QUERY_LENGTH)
        searchJob?.cancel()

        if (query.isBlank()) {
            results.clear()
            hasSearched = false
            isLoading = false
            error = null
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            search(channelId)
        }
    }

    fun search(channelId: String) {
        val current = query
        if (current.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val response = searchChannel(
                    channelId = channelId,
                    query = current,
                    includeUsers = true,
                    sort = sort.apiValue,
                    limit = 100
                )

                response.users?.forEach { user ->
                    user.id?.let { id ->
                        StoatAPI.userCache.putIfAbsent(id, user)
                    }
                }

                response.members?.forEach { member ->
                    member.id?.let { memberId ->
                        if (!StoatAPI.members.hasMember(memberId.server, memberId.user)) {
                            StoatAPI.members.setMember(memberId.server, member)
                        }
                    }
                }

                results.clear()
                results.addAll(response.messages ?: emptyList())
            } catch (e: Exception) {
                Log.e("ChannelSearchScreen", "Failed to search messages", e)
                error = e.message
            } finally {
                hasSearched = true
                isLoading = false
            }
        }
    }

    fun setSort(channelId: String, newSort: SearchSort) {
        if (newSort == sort) return
        sort = newSort
        if (query.isNotBlank()) search(channelId)
    }

    fun clear() {
        searchJob?.cancel()
        query = ""
        sort = SearchSort.RELEVANCE
        results.clear()
        hasSearched = false
        isLoading = false
        error = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSearchScreen(
    navController: NavController,
    channelId: String,
    modifier: Modifier = Modifier,
    viewModel: ChannelSearchScreenViewModel = viewModel()
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            inputField = {
                SearchBarDefaults.InputField(
                    query = viewModel.query,
                    onQueryChange = { viewModel.onQueryChange(channelId, it) },
                    onSearch = { viewModel.search(channelId) },
                    expanded = true,
                    onExpandedChange = { if (!it) navController.popBackStack() },
                    placeholder = { Text(stringResource(R.string.channel_search_hint)) },
                    leadingIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back_24dp),
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    trailingIcon = {
                        if (viewModel.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clear() }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close_24dp),
                                    contentDescription = stringResource(R.string.channel_search_clear)
                                )
                            }
                        }
                    }
                )
            },
            expanded = true,
            onExpandedChange = { if (!it) navController.popBackStack() },
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.background
            )
        ) {
            SearchResults(channelId = channelId, viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchResults(
    channelId: String,
    viewModel: ChannelSearchScreenViewModel
) {
    Column(modifier = Modifier.fillMaxHeight()) {
        if (viewModel.hasSearched) {
            SortChips(
                selected = viewModel.sort,
                onSelect = { viewModel.setSort(channelId, it) }
            )
        }

        Crossfade(targetState = viewModel.isLoading, label = "searchLoading") { loading ->
            when {
                loading -> CenteredColumn {
                    LoadingIndicator()
                }

                viewModel.error != null -> CenteredColumn {
                    Text(
                        text = viewModel.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 64.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.search(channelId) }) {
                        Text(stringResource(R.string.tap_to_retry))
                    }
                }

                !viewModel.hasSearched -> CenteredColumn {
                    Text(
                        text = stringResource(R.string.channel_search_prompt),
                        color = LocalContentColor.current,
                        modifier = Modifier.padding(horizontal = 64.dp),
                        textAlign = TextAlign.Center
                    )
                }

                viewModel.results.isEmpty() -> CenteredColumn {
                    Text(
                        text = stringResource(R.string.channel_search_empty, viewModel.query),
                        color = LocalContentColor.current,
                        modifier = Modifier.padding(horizontal = 64.dp),
                        textAlign = TextAlign.Center
                    )
                }

                else -> LazyColumn(
                    contentPadding = WindowInsets.navigationBars
                        .union(WindowInsets.ime)
                        .asPaddingValues()
                ) {
                    items(
                        viewModel.results.size,
                        key = { i -> viewModel.results[i].id ?: i }
                    ) { i ->
                        val message = viewModel.results[i].copy(tail = false)
                        if (message.system != null) {
                            SystemMessage(message)
                        } else {
                            chat.stoat.composables.chat.Message(message = message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChips(
    selected: SearchSort,
    onSelect: (SearchSort) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchSort.entries.forEach { sort ->
            FilterChip(
                selected = sort == selected,
                onClick = { onSelect(sort) },
                label = { Text(stringResource(sort.label)) }
            )
        }
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            content()
        }
    }
}
