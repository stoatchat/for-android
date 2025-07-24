package chat.revolt.screens.settings

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.internals.ChangelogIndex
import chat.revolt.internals.Changelogs
import chat.revolt.persistence.KVStorage
import chat.revolt.sheets.ChangelogSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

@HiltViewModel
class ChangelogsSettingsScreenViewModel @Inject constructor(
    val kvStorage: KVStorage,
    @ApplicationContext val context: Context
) : ViewModel() {
    var index by mutableStateOf<ChangelogIndex?>(null)
    var renderedChangelog by mutableStateOf("")

    suspend fun requestChangelog(version: String) {
        viewModelScope.launch {
            renderedChangelog = Changelogs(
                context,
                kvStorage
            ).fetchChangelogByVersionCode(version.toLong()).rendered
        }
    }

    suspend fun populate() {
        viewModelScope.launch {
            index = Changelogs(context, kvStorage).fetchChangelogIndex()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogsSettingsScreen(
    navController: NavController,
    viewModel: ChangelogsSettingsScreenViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.populate()
    }

    var currentChangelog by remember { mutableStateOf("") }
    var sheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(currentChangelog) {
        if (currentChangelog.isNotEmpty())
            viewModel.requestChangelog(currentChangelog)
    }

    if (sheetOpen) {
        val changelog =
            viewModel.index?.changelogs?.firstOrNull { it.version.code.toString() == currentChangelog }
                ?: return

        ChangelogSheet(
            versionName = changelog.version.name,
            versionIsHistorical = true,
            renderedContents = viewModel.renderedChangelog,
            onDismiss = {
                sheetOpen = false
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.settings_changelogs),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.icn_arrow_back_24dp),
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
            )
        },
    ) { pv ->
        Column(
            modifier = Modifier
                .padding(pv)
                .fillMaxSize()
        ) {
            Crossfade(targetState = viewModel.index, label = "index has items") { index ->
                if (index == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                    LazyColumn {
                        items(
                            viewModel.index?.changelogs?.size ?: 0,
                            key = { index ->
                                viewModel.index?.changelogs?.get(index)?.version?.name ?: ""
                            }
                        ) { index ->
                            val changelog = viewModel.index?.changelogs?.get(index) ?: return@items
                            val relativeTimeString = DateUtils.getRelativeTimeSpanString(
                                Instant.parse(changelog.date.publish).toEpochMilliseconds(),
                                System.currentTimeMillis(),
                                DateUtils.DAY_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL
                            )

                            Column(
                                modifier = Modifier
                                    .clickable {
                                        currentChangelog = changelog.version.code.toString()
                                        sheetOpen = true
                                    }
                                    .fillMaxWidth()
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = changelog.version.title,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    },
                                    supportingContent = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = changelog.summary,
                                            )
                                            Text(
                                                text = "${changelog.version.name} · $relativeTimeString",
                                                modifier = Modifier.alpha(0.7f),
                                            )
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
