package chat.stoat.screens.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import chat.stoat.BuildConfig
import chat.stoat.R
import chat.stoat.activities.InviteActivity
import chat.stoat.api.StoatAPI
import chat.stoat.api.settings.FeatureFlags
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.composables.generic.ListHeader
import chat.stoat.persistence.KVStorage
import kotlinx.coroutines.runBlocking
import org.koin.androidx.compose.koinViewModel

class SettingsScreenViewModel(
    private val kvStorage: KVStorage
) : ViewModel() {
    fun logout() {
        runBlocking {
            kvStorage.remove("sessionToken")
            kvStorage.remove("selfId")
            kvStorage.remove("selfName")
            kvStorage.remove("selfAvatarUrl")
            LoadedSettings.reset()
            StoatAPI.logout()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsScreenViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.settings),
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
                },
            )
        },
    ) { pv ->
        Box(Modifier.padding(pv)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 10.dp)
                ) {
                    ListHeader {
                        Text(stringResource(R.string.settings_category_account))
                    }

                    SettingsListItem(
                        first = true,
                        headlineContent = { Text(text = stringResource(id = R.string.settings_account)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lock_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_account")
                            .clickable { navController.navigate("settings/account") }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        headlineContent = { Text(text = stringResource(id = R.string.settings_profile)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.ic_id_card_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_profile")
                            .clickable { navController.navigate("settings/profile") }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        last = true,
                        headlineContent = { Text(text = stringResource(id = R.string.settings_sessions)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.ic_devices_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_sessions")
                            .clickable { navController.navigate("settings/sessions") }
                    )

                    ListHeader {
                        Text(stringResource(R.string.settings_category_general))
                    }

                    SettingsListItem(
                        first = true,
                        headlineContent = { Text(text = stringResource(id = R.string.settings_appearance)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.ic_palette_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_appearance")
                            .clickable { navController.navigate("settings/appearance") }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        headlineContent = { Text(text = stringResource(id = R.string.settings_language)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.ic_language_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_language")
                            .clickable { navController.navigate("settings/language") }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        headlineContent = { Text(text = stringResource(id = R.string.settings_chat)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.ic_chat_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_chat")
                            .clickable { navController.navigate("settings/chat") }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        last = true,
                        headlineContent = { Text(text = stringResource(id = R.string.settings_notifications)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.ic_notifications_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_notifications")
                            .clickable { navController.navigate("settings/notifications") }
                    )

                    ListHeader {
                        Text(stringResource(R.string.settings_category_miscellaneous))
                    }

                    val miscLastIsExperiments = LoadedSettings.experimentsEnabled
                    val miscLastIsLabs =
                        !miscLastIsExperiments && FeatureFlags.labsAccessControlGranted
                    val miscLastIsDebug =
                        !miscLastIsExperiments && !miscLastIsLabs && BuildConfig.DEBUG
                    val miscLastIsAbout =
                        !miscLastIsExperiments && !miscLastIsLabs && !miscLastIsDebug

                    SettingsListItem(
                        first = true,
                        last = miscLastIsAbout,
                        headlineContent = { Text(text = stringResource(id = R.string.about)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.ic_info_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_about")
                            .clickable { navController.navigate("about") }
                    )

                    if (BuildConfig.DEBUG) {
                        Spacer(Modifier.height(2.dp))
                        SettingsListItem(
                            last = miscLastIsDebug,
                            headlineContent = { Text(text = "Debug") },
                            leadingContent = {
                                SettingsIcon {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_sign_language_24dp),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier
                                .testTag("settings_view_debug")
                                .clickable { navController.navigate("settings/debug") }
                        )
                    }

                    if (FeatureFlags.labsAccessControlGranted) {
                        Spacer(Modifier.height(2.dp))
                        SettingsListItem(
                            last = miscLastIsLabs,
                            headlineContent = { Text(text = "Labs") },
                            leadingContent = {
                                SettingsIcon {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_sign_language_24dp),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier
                                .testTag("settings_view_labs")
                                .clickable { navController.navigate("labs") }
                        )
                    }

                    if (LoadedSettings.experimentsEnabled) {
                        Spacer(Modifier.height(2.dp))
                        SettingsListItem(
                            last = true,
                            headlineContent = { Text(text = "Experiments") },
                            leadingContent = {
                                SettingsIcon {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lab_research_24dp),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier
                                .testTag("settings_view_experiments")
                                .clickable { navController.navigate("settings/experiments") }
                        )
                    }

                    ListHeader {
                        Text(
                            stringResource(
                                R.string.settings_category_last,
                                BuildConfig.VERSION_NAME
                            )
                        )
                    }

                    SettingsListItem(
                        first = true,
                        headlineContent = { Text(text = stringResource(id = R.string.settings_changelog)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.ic_campaign_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_changelog")
                            .clickable { navController.navigate("changelog/latest") }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        headlineContent = { Text(text = stringResource(id = R.string.settings_feedback)) },
                        supportingContent = { Text(text = stringResource(id = R.string.settings_feedback_description)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.ic_feedback_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_feedback")
                            .clickable {
                                val intent = Intent(
                                    context,
                                    InviteActivity::class.java
                                ).setAction(Intent.ACTION_VIEW)
                                intent.data = "https://stt.gg/Testers".toUri()
                                context.startActivity(intent)
                            }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        last = true,
                        headlineContent = {
                            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                                Text(text = stringResource(id = R.string.logout))
                            }
                        },
                        leadingContent = {
                            SettingsIcon(danger = true) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_logout_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_logout")
                            .clickable {
                                viewModel.logout()
                                navController.navigate("login/greeting") {
                                    popUpTo("chat") {
                                        inclusive = true
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsListItem(
    first: Boolean = false,
    last: Boolean = false,
    headlineContent: @Composable () -> Unit,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(
                when {
                    first && last -> MaterialTheme.shapes.large
                    first -> MaterialTheme.shapes.extraSmall.copy(
                        topStart = MaterialTheme.shapes.large.topStart,
                        topEnd = MaterialTheme.shapes.large.topEnd
                    )

                    last -> MaterialTheme.shapes.extraSmall.copy(
                        bottomStart = MaterialTheme.shapes.large.bottomStart,
                        bottomEnd = MaterialTheme.shapes.large.bottomEnd
                    )

                    else -> MaterialTheme.shapes.extraSmall
                }
            )
            .then(modifier)
    )
}

@Composable
fun SettingsIcon(danger: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContentColor provides
                if (danger) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onBackground
    ) {
        content()
    }
}
