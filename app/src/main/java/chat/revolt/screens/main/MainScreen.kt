package chat.revolt.screens.main

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.screens.chat.views.OverviewScreen

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
                            painter = painterResource(
                                R.drawable.icn_tag_24dp
                            ),
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(stringResource(R.string.main_tab_communities))
                    }
                )
                NavigationBarItem(
                    selected = currentTab == MainScreenTab.Conversations,
                    onClick = { currentTab = MainScreenTab.Conversations },
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (currentTab == MainScreenTab.Conversations) {
                                    R.drawable.icn_forum_24dp__fill
                                } else {
                                    R.drawable.icn_forum_24dp
                                }
                            ),
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(stringResource(R.string.main_tab_conversations))
                    }
                )
                NavigationBarItem(
                    selected = currentTab == MainScreenTab.Overview,
                    onClick = { currentTab = MainScreenTab.Overview },
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (currentTab == MainScreenTab.Overview) {
                                    R.drawable.icn_star_shine_24dp__fill
                                } else {
                                    R.drawable.icn_star_shine_24dp
                                }
                            ),
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(stringResource(R.string.main_tab_overview))
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