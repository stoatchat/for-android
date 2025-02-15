package chat.revolt.screens.labs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

enum class LabsHomeScreenTab {
    Home,
    Mockups,
    Sandboxes,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabsHomeScreen(navController: NavController) {
    val currentTab = rememberSaveable { mutableStateOf(LabsHomeScreenTab.Home) }

    Scaffold(
        topBar = {
            TopAppBar(
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
                title = {
                    Text("Labs")
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab.value == LabsHomeScreenTab.Home,
                    onClick = { currentTab.value = LabsHomeScreenTab.Home },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text("Home")
                    }
                )
                NavigationBarItem(
                    selected = currentTab.value == LabsHomeScreenTab.Mockups,
                    onClick = { currentTab.value = LabsHomeScreenTab.Mockups },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text("UI Mockups")
                    }
                )
                NavigationBarItem(
                    selected = currentTab.value == LabsHomeScreenTab.Sandboxes,
                    onClick = { currentTab.value = LabsHomeScreenTab.Sandboxes },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text("Sandboxes")
                    }
                )
            }
        }
    ) {
        Box(Modifier.padding(it)) {
            when (currentTab.value) {
                LabsHomeScreenTab.Home -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Hey, this is kinda secret 🤫",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Remember, everything you see here can be broken and is not guaranteed to work.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Don't tell anyone about anything either, okay?",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                LabsHomeScreenTab.Mockups -> {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        ListItem(
                            headlineContent = {
                                Text("Call Screen")
                            },
                            modifier = Modifier.clickable {
                                navController.navigate("mockups/call")
                            }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = {
                                Text("New Login Experience")
                            },
                            modifier = Modifier.clickable {
                                navController.navigate("mockups/newlogin")
                            }
                        )
                        HorizontalDivider()
                    }
                }

                LabsHomeScreenTab.Sandboxes -> {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        ListItem(
                            headlineContent = {
                                Text("Settings DSL")
                            },
                            modifier = Modifier.clickable {
                                navController.navigate("sandboxes/settingsdsl")
                            }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = {
                                Text("JB Markdown")
                            },
                            modifier = Modifier.clickable {
                                navController.navigate("sandboxes/jbm")
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}