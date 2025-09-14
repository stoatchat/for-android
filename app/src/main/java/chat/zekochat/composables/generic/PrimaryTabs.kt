package chat.zekochat.composables.generic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import chat.zekochat.api.settings.LoadedSettings
import chat.zekochat.ui.theme.Theme
import com.google.android.material.tabs.TabLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimaryTabs(tabs: List<String>, currentIndex: Int, onTabSelected: (Int) -> Unit) {
    when (LoadedSettings.theme) {
        Theme.M3Dynamic -> AndroidView(
            factory = {
                TabLayout(it).apply {
                    tabMode = TabLayout.MODE_FIXED
                    tabGravity = TabLayout.GRAVITY_FILL

                    tabs.forEach { tab ->
                        addTab(newTab().setText(tab))
                    }

                    addOnTabSelectedListener(object :
                        TabLayout.OnTabSelectedListener {
                        override fun onTabSelected(tab: TabLayout.Tab?) {
                            onTabSelected(tab?.position ?: 0)
                        }

                        override fun onTabUnselected(tab: TabLayout.Tab?) {
                        }

                        override fun onTabReselected(tab: TabLayout.Tab?) {
                        }
                    })
                }
            },
            update = {
                it.getTabAt(currentIndex)?.select()
            },
            modifier = Modifier.fillMaxWidth()
        )

        else -> PrimaryTabRow(selectedTabIndex = currentIndex) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == currentIndex,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = tab,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (index == currentIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun PrimaryTabsPreview() {
    PrimaryTabs(
        tabs = listOf("Tab 1", "Tab 2", "Tab 3"),
        currentIndex = 0,
        onTabSelected = {}
    )
}
