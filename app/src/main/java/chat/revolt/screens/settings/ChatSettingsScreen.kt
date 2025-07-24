package chat.revolt.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.settings.LoadedSettings
import chat.revolt.api.settings.MessageReplyStyle
import chat.revolt.api.settings.SpecialEmbedSettings
import chat.revolt.api.settings.SyncedSettings
import chat.revolt.composables.generic.ListHeader
import chat.revolt.composables.generic.RadioItem
import kotlinx.coroutines.launch

class ChatSettingsScreenViewModel : ViewModel() {
    fun updateMessageReplyStyle(next: MessageReplyStyle) {
        viewModelScope.launch {
            SyncedSettings.updateAndroid(SyncedSettings.android.copy(messageReplyStyle = next.name))
            LoadedSettings.messageReplyStyle = next
        }
    }

    fun updateSpecialEmbedSettings(next: SpecialEmbedSettings) {
        viewModelScope.launch {
            SyncedSettings.updateAndroid(SyncedSettings.android.copy(specialEmbedSettings = next))
            LoadedSettings.specialEmbedSettings = next
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatSettingsScreen(
    navController: NavController,
    viewModel: ChatSettingsScreenViewModel = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.settings_chat),
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
        val scrollState = rememberScrollState()
        Column(
            Modifier
                .padding(pv)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            if (LoadedSettings.poorlyFormedSettingsKeys.isNotEmpty()) {
                Card(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_chat_hint_poorly_formed_settings_keys),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.settings_chat_hint_poorly_formed_settings_keys_description),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        for (key in LoadedSettings.poorlyFormedSettingsKeys) {
                            Text(
                                text = " • " + when (key) {
                                    "ordering" -> stringResource(R.string.settings_chat_hint_poorly_formed_settings_keys_key_ordering)
                                    "android" -> stringResource(R.string.settings_chat_hint_poorly_formed_settings_keys_key_android)
                                    "notifications" -> stringResource(R.string.settings_chat_hint_poorly_formed_settings_keys_key_notifications)
                                    else -> stringResource(
                                        R.string.settings_chat_hint_poorly_formed_settings_keys_key_unknown,
                                        key
                                    )
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        FlowRow(
                            modifier = Modifier.padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (key in LoadedSettings.poorlyFormedSettingsKeys.filter {
                                it in setOf("ordering", "android", "notifications")
                            }) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            when (key) {
                                                "ordering" -> SyncedSettings.resetOrdering()
                                                "android" -> SyncedSettings.resetAndroid()
                                                "notifications" -> SyncedSettings.resetNotifications()
                                            }
                                            LoadedSettings.poorlyFormedSettingsKeys -= key
                                        }
                                    }
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.settings_chat_hint_poorly_formed_settings_keys_reset,
                                            when (key) {
                                                "ordering" -> stringResource(R.string.settings_chat_hint_poorly_formed_settings_keys_key_ordering)
                                                "android" -> stringResource(R.string.settings_chat_hint_poorly_formed_settings_keys_key_android)
                                                "notifications" -> stringResource(R.string.settings_chat_hint_poorly_formed_settings_keys_key_notifications)
                                                else -> key
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ListHeader {
                Text(
                    text = stringResource(R.string.settings_chat_quick_reply)
                )
            }

            Column(Modifier.selectableGroup()) {
                RadioItem(
                    selected = LoadedSettings.messageReplyStyle == MessageReplyStyle.None,
                    onClick = { viewModel.updateMessageReplyStyle(MessageReplyStyle.None) },
                    label = { Text(text = stringResource(R.string.settings_chat_quick_reply_none)) },
                    description = {
                        Text(text = stringResource(R.string.settings_chat_quick_reply_none_description))
                    }
                )
                RadioItem(
                    selected = LoadedSettings.messageReplyStyle == MessageReplyStyle.SwipeFromEnd,
                    onClick = { viewModel.updateMessageReplyStyle(MessageReplyStyle.SwipeFromEnd) },
                    label = { Text(text = stringResource(R.string.settings_chat_quick_reply_swipe_from_end)) }
                )
                RadioItem(
                    selected = LoadedSettings.messageReplyStyle == MessageReplyStyle.DoubleTap,
                    onClick = { viewModel.updateMessageReplyStyle(MessageReplyStyle.DoubleTap) },
                    label = { Text(text = stringResource(R.string.settings_chat_quick_reply_double_tap)) }
                )
            }

            ListHeader {
                Text(
                    text = stringResource(R.string.settings_chat_interactive_embeds)
                )
            }

            Text(
                stringResource(R.string.settings_chat_interactive_embeds_description),
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp),
            )

            Column {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_chat_interactive_embeds_youtube))
                    },
                    trailingContent = {
                        Switch(
                            checked = LoadedSettings.specialEmbedSettings.embedYouTube,
                            onCheckedChange = null
                        )
                    },
                    modifier = Modifier.clickable {
                        viewModel.updateSpecialEmbedSettings(
                            LoadedSettings.specialEmbedSettings.copy(
                                embedYouTube = !LoadedSettings.specialEmbedSettings.embedYouTube
                            )
                        )
                    }
                )

                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_chat_interactive_embeds_apple_music))
                    },
                    trailingContent = {
                        Switch(
                            checked = LoadedSettings.specialEmbedSettings.embedAppleMusic,
                            onCheckedChange = null
                        )
                    },
                    modifier = Modifier.clickable {
                        viewModel.updateSpecialEmbedSettings(
                            LoadedSettings.specialEmbedSettings.copy(
                                embedAppleMusic = !LoadedSettings.specialEmbedSettings.embedAppleMusic
                            )
                        )
                    }
                )
            }
        }
    }
}