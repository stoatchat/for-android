package chat.revolt.screens.settings

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.auth.fetchAllSessions
import chat.revolt.api.routes.auth.logoutAllSessions
import chat.revolt.api.routes.auth.logoutSessionById
import chat.revolt.api.schemas.Session
import chat.revolt.composables.generic.ListHeader
import chat.revolt.composables.markdown.RichMarkdown
import chat.revolt.composables.settings.sessions.SessionItem
import kotlinx.coroutines.launch

class SessionSettingsScreenViewModel : ViewModel() {
    var isLoading by mutableStateOf(true)
    val sessions = mutableStateListOf<Session>()
    var currentSession by mutableStateOf<Session?>(null)
    var showLogoutOtherConfirmation by mutableStateOf(false)

    fun fetchSessions() {
        viewModelScope.launch {
            sessions.addAll(fetchAllSessions())
            currentSession = sessions.firstOrNull { it.isCurrent() }
            Log.d(
                "SessionSettingsScreen",
                "Current session: $currentSession. Current session ID: ${RevoltAPI.sessionId}"
            )
            isLoading = false
        }
    }

    fun logoutSession(id: String) {
        viewModelScope.launch {
            logoutSessionById(id)
            sessions.removeIf { it.id == id }
        }
    }

    fun logoutOtherSessions() {
        viewModelScope.launch {
            logoutAllSessions(includingSelf = false)
            sessions.clear()
            sessions.addAll(fetchAllSessions())
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SessionSettingsScreen(
    navController: NavController,
    viewModel: SessionSettingsScreenViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.fetchSessions()
    }

    if (viewModel.showLogoutOtherConfirmation) {
        AlertDialog(
            onDismissRequest = {
                viewModel.showLogoutOtherConfirmation = false
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_sessions_log_out_other_confirm)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.showLogoutOtherConfirmation = false
                        viewModel.logoutOtherSessions()
                    }
                ) {
                    Text(stringResource(R.string.settings_sessions_log_out_other_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.showLogoutOtherConfirmation = false
                    }
                ) {
                    Text(stringResource(R.string.settings_sessions_log_out_other_confirm_no))
                }
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
                        text = stringResource(R.string.settings_sessions),
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
        Box(Modifier.padding(pv)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (viewModel.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(48.dp)
                        )
                    }
                } else {
                    LazyColumn {
                        stickyHeader(key = "thisDevice") {
                            ListHeader {
                                Text(stringResource(id = R.string.settings_sessions_this_device))
                            }
                        }

                        viewModel.currentSession?.let {
                            item(key = it.id) {
                                Spacer(Modifier.height(8.dp))
                                SessionItem(
                                    session = it,
                                    currentSession = true,
                                    onLogout = {},
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        } ?: run {
                            item(key = "noCurrentSession") {
                                RichMarkdown(
                                    input = stringResource(id = R.string.settings_sessions_this_device_unavailable),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(16.dp)
                                )
                            }
                        }

                        stickyHeader(key = "otherSessions") {
                            ListHeader {
                                Text(stringResource(id = R.string.settings_sessions_other_sessions))
                            }
                        }

                        item(key = "logoutOtherSessions") {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .clip(shape = MaterialTheme.shapes.medium)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 16.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_sessions_log_out_other),
                                        style = MaterialTheme.typography.labelLarge
                                    )

                                    Text(
                                        text = stringResource(
                                            R.string.settings_sessions_log_out_other_description
                                        ),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Normal
                                        )
                                    )
                                }

                                FilledTonalButton(onClick = {
                                    viewModel.showLogoutOtherConfirmation = true
                                }) {
                                    Text(stringResource(R.string.logout))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        items(viewModel.sessions.size) {
                            val item = viewModel.sessions[it]

                            if (item.isCurrent()) {
                                return@items
                            }

                            SessionItem(
                                session = item,
                                onLogout = { session ->
                                    viewModel.logoutSession(session.id)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}
