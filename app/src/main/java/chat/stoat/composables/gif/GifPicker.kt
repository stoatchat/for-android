package chat.stoat.composables.gif

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.stoat.api.routes.microservices.gifbox.GifCategory
import chat.stoat.api.routes.microservices.gifbox.GifResult
import chat.stoat.api.routes.microservices.gifbox.Gifbox
import chat.stoat.composables.generic.RemoteImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GifPicker(
    onGifSelected: (String) -> Unit,
    onSearchFocus: (Boolean) -> Unit = {},
    bottomInset: Dp = 0.dp
) {
    var searchQuery by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<GifCategory>>(emptyList()) }
    var results by remember { mutableStateOf<List<GifResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var activeQuery by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Load categories on mount
    LaunchedEffect(Unit) {
        try {
            categories = Gifbox.fetchCategories()
        } catch (_: Exception) { }
        isLoading = false
    }

    // Debounced search
    fun doSearch(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            activeQuery = null
            results = emptyList()
            return
        }
        searchJob = scope.launch {
            delay(400)
            activeQuery = query
            isLoading = true
            try {
                results = Gifbox.search(query).results
            } catch (_: Exception) {
                results = emptyList()
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        // Search bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            val textFieldState = rememberTextFieldState()
            BasicTextField(
                state = textFieldState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .onFocusChanged { onSearchFocus(it.isFocused) },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                ),
                decorator = { innerTextField ->
                    if (textFieldState.text.isEmpty()) {
                        Text(
                            "Search GIFs...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                },
                onTextLayout = { },
            )

            // Sync text field state with search query
            LaunchedEffect(textFieldState.text) {
                val newText = textFieldState.text.toString()
                if (newText != searchQuery) {
                    searchQuery = newText
                    doSearch(newText)
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else if (activeQuery != null) {
            // Search results
            GifGrid(
                gifs = results,
                onGifSelected = onGifSelected,
                modifier = Modifier.weight(1f),
                bottomInset = bottomInset
            )
        } else {
            // Category grid
            CategoryGrid(
                categories = categories,
                onCategorySelected = { category ->
                    searchQuery = category.title
                    doSearch(category.title)
                },
                modifier = Modifier.weight(1f),
                bottomInset = bottomInset
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<GifCategory>,
    onCategorySelected: (GifCategory) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp + bottomInset),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        items(categories, key = { it.title }) { category ->
            Box(
                modifier = Modifier
                    .aspectRatio(5f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCategorySelected(category) }
            ) {
                RemoteImage(
                    url = category.image,
                    description = category.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GifGrid(
    gifs: List<GifResult>,
    onGifSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp
) {
    if (gifs.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No GIFs found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp + bottomInset),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        items(gifs, key = { it.url }) { gif ->
            val previewUrl = gif.mediaFormats["tinywebm"]?.url
                ?: gif.mediaFormats["webm"]?.url
                ?: gif.url

            Box(
                modifier = Modifier
                    .aspectRatio(5f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onGifSelected(gif.url) }
            ) {
                RemoteImage(
                    url = previewUrl,
                    description = "GIF",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
