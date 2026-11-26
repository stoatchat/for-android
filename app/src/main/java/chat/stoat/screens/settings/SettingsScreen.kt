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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import chat.stoat.activities.InviteActivity
import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatAPIHost
import chat.stoat.api.settings.FeatureFlags
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.core.designsystem.components.ListHeader
import chat.stoat.core.designsystem.icons.StoatIcons
import chat.stoat.core.designsystem.icons.painter
import chat.stoat.navigation.Navigator
import chat.stoat.navigation.Route
import chat.stoat.persistence.KVStorage
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.about
import chat.stoat.shared.resources.back
import chat.stoat.shared.resources.logout
import chat.stoat.shared.resources.settings
import chat.stoat.shared.resources.settings_account
import chat.stoat.shared.resources.settings_appearance
import chat.stoat.shared.resources.settings_category_account
import chat.stoat.shared.resources.settings_category_general
import chat.stoat.shared.resources.settings_category_last
import chat.stoat.shared.resources.settings_category_miscellaneous
import chat.stoat.shared.resources.settings_changelog
import chat.stoat.shared.resources.settings_chat
import chat.stoat.shared.resources.settings_feedback
import chat.stoat.shared.resources.settings_feedback_description
import chat.stoat.shared.resources.settings_language
import chat.stoat.shared.resources.settings_notifications
import chat.stoat.shared.resources.settings_profile
import chat.stoat.shared.resources.settings_sessions
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

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
    navigator: Navigator,
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
                        text = stringResource(Res.string.settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navigator.popBackStack()
                    }) {
                        Icon(
                            painter = StoatIcons.Back.painter(),
                            contentDescription = stringResource(Res.string.back)
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
                        Text(stringResource(Res.string.settings_category_account))
                    }

                    SettingsListItem(
                        first = true,
                        headlineContent = { Text(text = stringResource(Res.string.settings_account)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = StoatIcons.Account.painter(),
                                    contentDescription = null,
                                )
                            }
                        },
                        detailRoute = Route.Settings.Account,
                        modifier = Modifier
                            .testTag("settings_view_account")
                            .clickable { navigator.navigate(Route.Settings.Account) }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        headlineContent = { Text(text = stringResource(Res.string.settings_profile)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = StoatIcons.Profile.painter(),
                                    contentDescription = null,
                                )
                            }
                        },
                        detailRoute = Route.Settings.Profile,
                        modifier = Modifier
                            .testTag("settings_view_profile")
                            .clickable { navigator.navigate(Route.Settings.Profile) }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        last = true,
                        headlineContent = { Text(text = stringResource(Res.string.settings_sessions)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = StoatIcons.Sessions.painter(),
                                    contentDescription = null,
                                )
                            }
                        },
                        detailRoute = Route.Settings.Sessions,
                        modifier = Modifier
                            .testTag("settings_view_sessions")
                            .clickable { navigator.navigate(Route.Settings.Sessions) }
                    )

                    ListHeader {
                        Text(stringResource(Res.string.settings_category_general))
                    }

                    SettingsListItem(
                        first = true,
                        headlineContent = { Text(text = stringResource(Res.string.settings_appearance)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = StoatIcons.Appearance.painter(),
                                    contentDescription = null,
                                )
                            }
                        },
                        detailRoute = Route.Settings.Appearance,
                        modifier = Modifier
                            .testTag("settings_view_appearance")
                            .clickable { navigator.navigate(Route.Settings.Appearance) }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        headlineContent = { Text(text = stringResource(Res.string.settings_language)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = StoatIcons.Language.painter(),
                                    contentDescription = null,
                                )
                            }
                        },
                        detailRoute = Route.Settings.Language,
                        modifier = Modifier
                            .testTag("settings_view_language")
                            .clickable { navigator.navigate(Route.Settings.Language) }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        headlineContent = { Text(text = stringResource(Res.string.settings_chat)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = StoatIcons.ChatSettings.painter(),
                                    contentDescription = null,
                                )
                            }
                        },
                        detailRoute = Route.Settings.Chat,
                        modifier = Modifier
                            .testTag("settings_view_chat")
                            .clickable { navigator.navigate(Route.Settings.Chat) }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        last = true,
                        headlineContent = { Text(text = stringResource(Res.string.settings_notifications)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = StoatIcons.Notifications.painter(),
                                    contentDescription = null,
                                )
                            }
                        },
                        detailRoute = Route.Settings.Notifications,
                        modifier = Modifier
                            .testTag("settings_view_notifications")
                            .clickable { navigator.navigate(Route.Settings.Notifications) }
                    )

                    ListHeader {
                        Text(stringResource(Res.string.settings_category_miscellaneous))
                    }

                    val miscLastIsExperiments = LoadedSettings.experimentsEnabled
                    val miscLastIsLabs =
                        !miscLastIsExperiments && FeatureFlags.labsAccessControlGranted
                    val miscLastIsDebug =
                        !miscLastIsExperiments && !miscLastIsLabs && StoatAPIHost.config.isDebug
                    val miscLastIsAbout =
                        !miscLastIsExperiments && !miscLastIsLabs && !miscLastIsDebug

                    SettingsListItem(
                        first = true,
                        last = miscLastIsAbout,
                        headlineContent = { Text(text = stringResource(Res.string.about)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = StoatIcons.About.painter(),
                                    contentDescription = null,
                                )
                            }
                        },
                        detailRoute = Route.About.Home,
                        modifier = Modifier
                            .testTag("settings_view_about")
                            .clickable { navigator.navigate(Route.About.Home) }
                    )

                    if (StoatAPIHost.config.isDebug) {
                        Spacer(Modifier.height(2.dp))
                        SettingsListItem(
                            last = miscLastIsDebug,
                            headlineContent = { Text(text = "Debug") },
                            leadingContent = {
                                SettingsIcon {
                                    Icon(
                                        painter = StoatIcons.Debug.painter(),
                                        contentDescription = null,
                                    )
                                }
                            },
                            detailRoute = Route.Settings.Debug,
                            modifier = Modifier
                                .testTag("settings_view_debug")
                                .clickable { navigator.navigate(Route.Settings.Debug) }
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
                                        painter = StoatIcons.Labs.painter(),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier
                                .testTag("settings_view_labs")
                                .clickable { navigator.navigate(Route.Labs) }
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
                                        painter = StoatIcons.Experiments.painter(),
                                        contentDescription = null,
                                    )
                                }
                            },
                            detailRoute = Route.Settings.Experiments,
                            modifier = Modifier
                                .testTag("settings_view_experiments")
                                .clickable { navigator.navigate(Route.Settings.Experiments) }
                        )
                    }

                    ListHeader {
                        Text(
                            stringResource(
                                Res.string.settings_category_last,
                                StoatAPIHost.config.versionName
                            )
                        )
                    }

                    SettingsListItem(
                        first = true,
                        headlineContent = { Text(text = stringResource(Res.string.settings_changelog)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = StoatIcons.Changelog.painter(),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_changelog")
                            .clickable { navigator.navigate(Route.Changelog("latest")) }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        headlineContent = { Text(text = stringResource(Res.string.settings_feedback)) },
                        supportingContent = { Text(text = stringResource(Res.string.settings_feedback_description)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = StoatIcons.Feedback.painter(),
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
                                Text(text = stringResource(Res.string.logout))
                            }
                        },
                        leadingContent = {
                            SettingsIcon(danger = true) {
                                Icon(
                                    painter = StoatIcons.LogOut.painter(),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_logout")
                            .clickable {
                                viewModel.logout()
                                navigator.resetTo(Route.Login.Greeting)
                            }
                    )
                }
            }
        }
    }
}
