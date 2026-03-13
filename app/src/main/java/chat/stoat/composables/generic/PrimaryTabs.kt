package chat.stoat.composables.generic

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimaryTabs(tabs: List<String>, currentIndex: Int, onTabSelected: (Int) -> Unit) {
    PrimaryTabRow(selectedTabIndex = currentIndex) {
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

@Preview
@Composable
fun PrimaryTabsPreview() {
    PrimaryTabs(
        tabs = listOf("Tab 1", "Tab 2", "Tab 3"),
        currentIndex = 0,
        onTabSelected = {}
    )
}
