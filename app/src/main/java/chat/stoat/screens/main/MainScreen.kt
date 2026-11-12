package chat.stoat.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import chat.stoat.core.designsystem.icons.StoatIcons
import chat.stoat.core.designsystem.icons.painter
import chat.stoat.screens.chat.views.OverviewScreen
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.main_tab_communities
import chat.stoat.shared.resources.main_tab_conversations
import chat.stoat.shared.resources.main_tab_overview
import org.jetbrains.compose.resources.stringResource

enum class MainScreenTab {
    Communities,
    Conversations,
    Overview
}

@Composable
fun MainScreen(navController: NavController) {
    var currentTab by rememberSaveable { mutableStateOf(MainScreenTab.Communities) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == MainScreenTab.Communities,
                    onClick = { currentTab = MainScreenTab.Communities },
                    icon = {
                        Icon(
                            painter = StoatIcons.TabCommunities.painter(),
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(stringResource(Res.string.main_tab_communities))
                    }
                )
                NavigationBarItem(
                    selected = currentTab == MainScreenTab.Conversations,
                    onClick = { currentTab = MainScreenTab.Conversations },
                    icon = {
                        Icon(
                            painter = if (currentTab == MainScreenTab.Conversations) {
                                StoatIcons.TabConversationsSelected.painter()
                            } else {
                                StoatIcons.TabConversations.painter()
                            },
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(stringResource(Res.string.main_tab_conversations))
                    }
                )
                NavigationBarItem(
                    selected = currentTab == MainScreenTab.Overview,
                    onClick = { currentTab = MainScreenTab.Overview },
                    icon = {
                        Icon(
                            painter = if (currentTab == MainScreenTab.Overview) {
                                StoatIcons.TabOverviewSelected.painter()
                            } else {
                                StoatIcons.TabOverview.painter()
                            },
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(stringResource(Res.string.main_tab_overview))
                    }
                )
            }
        },
    ) { pv ->
        Box(Modifier.padding(pv)) {
            when (currentTab) {
                MainScreenTab.Communities -> {}
                MainScreenTab.Conversations -> {
                    ConversationsScreen(
                        navController
                    )
                }

                MainScreenTab.Overview -> {
                    OverviewScreen(
                        navController,
                        useDrawer = false,
                        onDrawerClicked = {},
                        includePadding = false
                    )
                }
            }
        }
    }
}