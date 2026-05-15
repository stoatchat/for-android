package chat.stoat.screens.changelogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.routes.microservices.gazette.GazetteChangelog
import chat.stoat.api.routes.microservices.gazette.getChangelogById
import chat.stoat.composables.markdown.prose.ProseMarkdown
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

sealed interface ReadChangelogScreenUiState {
    data object Loading : ReadChangelogScreenUiState
    data class Success(val changelog: GazetteChangelog) : ReadChangelogScreenUiState
    data class Error(val throwable: Throwable) : ReadChangelogScreenUiState
}

class ReadChangelogScreenViewModel(
    handle: SavedStateHandle
) : ViewModel() {
    private val id: String = checkNotNull(handle["id"])
    private val retry = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ReadChangelogScreenUiState> = retry
        .flatMapLatest {
            flow { emit(ReadChangelogScreenUiState.Success(getChangelogById(id)) as ReadChangelogScreenUiState) }
                .catch { emit(ReadChangelogScreenUiState.Error(it)) }
                .onStart { emit(ReadChangelogScreenUiState.Loading) }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ReadChangelogScreenUiState.Loading
        )

    fun retry() {
        retry.tryEmit(Unit)
    }
}

@Composable
private fun rememberDateFormatter(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
}

@Composable
private fun rememberFormattedDate(isoDateTime: String?): String? {
    val formatter = rememberDateFormatter()
    val zone = remember { ZoneId.systemDefault() }
    return remember(isoDateTime, formatter, zone) {
        isoDateTime?.let {
            Instant.parse(it).atZone(zone).toLocalDate().format(formatter)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReadChangelogScreen(
    navController: NavController,
    viewModel: ReadChangelogScreenViewModel = viewModel()
) {
    val state = viewModel.state.collectAsState()
    val formattedDate =
        rememberFormattedDate((state.value as? ReadChangelogScreenUiState.Success)?.changelog?.publishedAt)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.changelog_header),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                subtitle = {
                    Box(
                        Modifier.fillMaxWidth(),
                    ) {
                        AnimatedVisibility(
                            visible = state.value is ReadChangelogScreenUiState.Success,
                            enter = fadeIn() + expandIn(expandFrom = Alignment.BottomCenter),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                formattedDate ?: stringResource(R.string.unknown),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { pv ->
        Box(Modifier
            .fillMaxSize()
            .padding(pv)) {
            AnimatedContent(
                targetState = state.value,
                modifier = Modifier.fillMaxSize()
            ) { uiState ->
                when (uiState) {
                    is ReadChangelogScreenUiState.Loading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator(
                            modifier = Modifier.size(72.dp)
                        )
                    }

                    is ReadChangelogScreenUiState.Error -> Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            stringResource(R.string.changelog_load_error),
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = { viewModel.retry() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(stringResource(R.string.changelog_retry))
                        }
                    }

                    is ReadChangelogScreenUiState.Success -> {
                        ProseMarkdown(
                            markdownText = uiState.changelog.markdownContent,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}