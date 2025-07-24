package chat.revolt.composables.emoji

import android.util.TypedValue
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.activities.RevoltTweenFloat
import chat.revolt.api.REVOLT_FILES
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.composables.generic.IconPlaceholder
import chat.revolt.composables.generic.RemoteImage
import chat.revolt.internals.Category
import chat.revolt.internals.EmojiImpl
import chat.revolt.internals.EmojiPickerItem
import chat.revolt.internals.FitzpatrickSkinTone
import chat.revolt.internals.UnicodeEmojiSection
import kotlinx.coroutines.launch

@Composable
fun EmojiPicker(
    onSearchFocus: (Boolean) -> Unit = {},
    bottomInset: Dp = 0.dp,
    onEmojiSelected: (String) -> Unit,
) {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current

    val emojiImpl = remember { EmojiImpl() }
    val pickerList = remember(emojiImpl) { emojiImpl.flatPickerList() }
    val servers = remember(emojiImpl) { emojiImpl.serversWithEmotes() }
    val categorySpans = remember(pickerList) { emojiImpl.categorySpans(pickerList) }

    val gridState = rememberLazyGridState()
    val categoryRowScrollState = rememberScrollState()

    val scope = rememberCoroutineScope()

    val spanCount = 9 // https://github.com/googlefonts/emoji-metadata/#readme

    // The current category is the one that the user is currently looking at.
    val currentCategory = remember(gridState, categorySpans) {
        derivedStateOf {
            val firstVisible = gridState.firstVisibleItemIndex
            val firstCategory =
                categorySpans.entries.firstOrNull {
                    it.value.first <= firstVisible && it.value.second >= firstVisible
                }?.key

            firstCategory
        }
    }

    LaunchedEffect(currentCategory.value) {
        // Scroll to the server icon of the current category.
        val offset = categorySpans.entries.indexOfFirst { it.key == currentCategory.value }
        var px = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            37f + 4f,
            view.resources.displayMetrics
        ).toInt()

        // If the user is looking at the unicode emoji, scroll to the end instead
        // so that the category icons are all neatly aligned.
        //
        // Impl -> Not scrolling to "the end" but to the current category plus 50.
        // (Which technically is an evil hack, but technically I could also
        // poke an eye out with a spoon, so let's not worry about technicalities.)
        if (currentCategory.value is Category.UnicodeEmojiCategory) px += 50

        categoryRowScrollState.animateScrollTo(offset * px)
    }

    var currentSkinTone by remember { mutableStateOf(FitzpatrickSkinTone.None) }
    var showSkinToneMenu by remember { mutableStateOf(false) }
    val skinToneMenuAreaWeight by animateFloatAsState(
        if (showSkinToneMenu) 1f else .15f,
        animationSpec = RevoltTweenFloat,
        label = "skinToneMenuAreaWeight"
    )
    val skinToneMenuCloseHintIconOpacity by animateFloatAsState(
        if (showSkinToneMenu) 1f else 0f,
        animationSpec = RevoltTweenFloat,
        label = "skinToneMenuCloseHintIconOpacity"
    )

    val skinSample = remember(pickerList) {
        pickerList
            .filterIsInstance<EmojiPickerItem.UnicodeEmoji>()
            .first { it.character == "\uD83E\uDEF0" }
    }

    var searchQuery by remember { mutableStateOf("") }
    val searchFieldOpacity by animateFloatAsState(
        if (showSkinToneMenu) 0f else 1f,
        animationSpec = RevoltTweenFloat,
        label = "searchFieldOpacity"
    )

    val searchResults = remember { mutableStateListOf<EmojiPickerItem>() }
    LaunchedEffect(searchQuery) {
        searchResults.clear()
        if (searchQuery.isBlank()) return@LaunchedEffect
        searchResults.addAll(emojiImpl.searchForEmoji(searchQuery))
        gridState.scrollToItem(0)
    }

    val onServerEmoteInfo: (String) -> Unit = {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        scope.launch {
            ActionChannel.send(
                Action.EmoteInfo(
                    it
                )
            )
        }
    }
    val onEmojiClick: (EmojiPickerItem) -> Unit = {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        when (it) {
            is EmojiPickerItem.UnicodeEmoji -> onEmojiSelected(
                emojiImpl.applyFitzpatrickSkinTone(
                    it,
                    currentSkinTone
                )
            )

            is EmojiPickerItem.ServerEmote -> onEmojiSelected(":${it.emote.id}:")
            else -> {}
        }
    }
    val clearQueryButtonOpacity = animateFloatAsState(
        if (searchQuery.isNotEmpty()) 1f else 0f,
        animationSpec = RevoltTweenFloat,
        label = "clearQueryButtonOpacity"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(37.dp)
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                readOnly = showSkinToneMenu,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(.9f)
                    .alpha(searchFieldOpacity)
                    .align(Alignment.CenterStart)
                    .onFocusChanged {
                        onSearchFocus(it.isFocused)
                    }
            ) { innerTextField ->
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    innerTextField()

                    Icon(
                        painter = painterResource(R.drawable.icn_close_24dp),
                        contentDescription = stringResource(R.string.emoji_picker_clear_search),
                        modifier = Modifier
                            .clip(CircleShape)
                            .padding(4.dp)
                            .size(24.dp)
                            .then(
                                if (searchQuery.isNotEmpty()) {
                                    Modifier.clickable {
                                        searchQuery = ""
                                        focusManager.clearFocus() // this prevents the text field Z-below from gaining focus
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .align(Alignment.CenterEnd)
                            .alpha(clearQueryButtonOpacity.value)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .height(37.dp)
                    .fillMaxWidth(skinToneMenuAreaWeight)
                    .align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))

                AnimatedVisibility(
                    showSkinToneMenu
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FitzpatrickSkinTone.entries.forEach { skinTone ->
                            Text(
                                emojiImpl.applyFitzpatrickSkinTone(
                                    skinSample,
                                    skinTone
                                ),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .requiredSize(24.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        onClickLabel = when (skinTone) {
                                            FitzpatrickSkinTone.None -> stringResource(
                                                R.string.emoji_picker_skin_tone_none
                                            )

                                            FitzpatrickSkinTone.Light -> stringResource(
                                                R.string.emoji_picker_skin_tone_fitzpatrick_1_2
                                            )

                                            FitzpatrickSkinTone.MediumLight -> stringResource(
                                                R.string.emoji_picker_skin_tone_fitzpatrick_3
                                            )

                                            FitzpatrickSkinTone.Medium -> stringResource(
                                                R.string.emoji_picker_skin_tone_fitzpatrick_4
                                            )

                                            FitzpatrickSkinTone.MediumDark -> stringResource(
                                                R.string.emoji_picker_skin_tone_fitzpatrick_5
                                            )

                                            FitzpatrickSkinTone.Dark -> stringResource(
                                                R.string.emoji_picker_skin_tone_fitzpatrick_6
                                            )
                                        }
                                    ) {
                                        currentSkinTone = skinTone
                                        showSkinToneMenu = false
                                        focusManager.clearFocus() // this prevents the text field Z-below from gaining focus
                                    }
                                    .aspectRatio(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .requiredSize(24.dp)
                        .clip(CircleShape)
                        .clickable {
                            showSkinToneMenu = !showSkinToneMenu
                        }
                        .aspectRatio(1f)
                ) {
                    Text(
                        emojiImpl.applyFitzpatrickSkinTone(
                            skinSample,
                            currentSkinTone
                        ),
                        modifier = Modifier
                            .padding(4.dp)
                            .requiredSize(24.dp)
                            .clip(CircleShape)
                            .aspectRatio(1f)
                            .alpha(1f - skinToneMenuCloseHintIconOpacity),
                        textAlign = TextAlign.Center
                    )
                    Icon(
                        painter = painterResource(R.drawable.icn_keyboard_arrow_right_24dp),
                        contentDescription = stringResource(
                            R.string.emoji_picker_close_skin_tone_menu
                        ),
                        tint = LocalContentColor.current,
                        modifier = Modifier
                            .alpha(skinToneMenuCloseHintIconOpacity)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        AnimatedVisibility(searchResults.isEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(categoryRowScrollState)
                    .padding(vertical = 4.dp)
                    .height(37.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                servers.forEach { server ->
                    Column(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                scope.launch {
                                    val index =
                                        pickerList.indexOfFirst {
                                            it is EmojiPickerItem.Section && it.category is Category.ServerEmoteCategory && it.category.server == server
                                        }
                                    gridState.scrollToItem(index)
                                }
                            }
                            .then(
                                if (currentCategory.value is Category.ServerEmoteCategory && (currentCategory.value as Category.ServerEmoteCategory).server == server) {
                                    Modifier.background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .aspectRatio(1f)
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (server.icon == null) {
                            IconPlaceholder(
                                name = server.name ?: stringResource(R.string.unknown),
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .fillMaxSize()
                            )
                        } else {
                            RemoteImage(
                                url = "$REVOLT_FILES/icons/${server.icon.id}",
                                allowAnimation = false,
                                description = server.name,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
                UnicodeEmojiSection.entries.forEach { category ->
                    Column(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                scope.launch {
                                    val index =
                                        pickerList.indexOfFirst {
                                            it is EmojiPickerItem.Section && it.category is Category.UnicodeEmojiCategory && it.category.definition == category
                                        }
                                    gridState.scrollToItem(index)
                                }
                            }
                            .then(
                                if (currentCategory.value is Category.UnicodeEmojiCategory && (currentCategory.value as Category.UnicodeEmojiCategory).definition == category) {
                                    Modifier.background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .aspectRatio(1f)
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = when (category) {
                                UnicodeEmojiSection.Smileys -> painterResource(
                                    R.drawable.icn_mood_24dp
                                )

                                UnicodeEmojiSection.People -> painterResource(
                                    R.drawable.icn_emoji_people_24dp
                                )

                                UnicodeEmojiSection.Animals -> painterResource(
                                    R.drawable.icn_emoji_nature_24dp
                                )

                                UnicodeEmojiSection.Food -> painterResource(
                                    R.drawable.icn_sports_bar_24dp
                                )

                                UnicodeEmojiSection.Travel -> painterResource(
                                    R.drawable.icn_bus_railway_24dp
                                )

                                UnicodeEmojiSection.Activities -> painterResource(
                                    R.drawable.icn_sports_and_outdoors_24dp
                                )

                                UnicodeEmojiSection.Objects -> painterResource(
                                    R.drawable.icn_emoji_objects_24dp
                                )

                                UnicodeEmojiSection.Symbols -> painterResource(
                                    R.drawable.icn_emoji_symbols_24dp
                                )

                                UnicodeEmojiSection.Flags -> painterResource(
                                    R.drawable.icn_flag_24dp
                                )
                            },
                            contentDescription = null,
                            tint = if (currentCategory.value is Category.UnicodeEmojiCategory && (currentCategory.value as Category.UnicodeEmojiCategory).definition == category) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                LocalContentColor.current
                            }
                        )
                    }
                }
            }
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(spanCount),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (searchResults.isNotEmpty()) {
                item(
                    key = "searchResultsHeader",
                    span = {
                        GridItemSpan(spanCount)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.emoji_picker_search_results_header),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }

            // Search results do not get a key, this is intentional.
            items(
                searchResults.size,
                span = {
                    val item = searchResults[it]
                    when (item) {
                        is EmojiPickerItem.UnicodeEmoji -> GridItemSpan(1)
                        is EmojiPickerItem.ServerEmote -> GridItemSpan(1)
                        is EmojiPickerItem.Section -> GridItemSpan(spanCount)
                    }
                }
            ) { index ->
                PickerItem(
                    item = searchResults[index],
                    skinToneFactory = { emojiImpl.applyFitzpatrickSkinTone(it, currentSkinTone) },
                    onClick = onEmojiClick,
                    onServerEmoteInfo = onServerEmoteInfo,
                    lesserHeaders = true
                )
            }

            if (searchResults.isNotEmpty()) {
                item(
                    key = "searchResultsFooter",
                    span = {
                        GridItemSpan(spanCount)
                    }
                ) {
                    HorizontalDivider()
                }
            }

            items(
                pickerList.size,
                span = {
                    val item = pickerList[it]
                    when (item) {
                        is EmojiPickerItem.UnicodeEmoji -> GridItemSpan(1)
                        is EmojiPickerItem.ServerEmote -> GridItemSpan(1)
                        is EmojiPickerItem.Section -> GridItemSpan(spanCount)
                    }
                }
            ) { index ->
                PickerItem(
                    item = pickerList[index],
                    skinToneFactory = { emojiImpl.applyFitzpatrickSkinTone(it, currentSkinTone) },
                    onClick = onEmojiClick,
                    onServerEmoteInfo = onServerEmoteInfo
                )
            }

            item(
                key = "bottomInset",
                span = {
                    GridItemSpan(spanCount)
                }
            ) {
                Spacer(Modifier.height(bottomInset))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.PickerItem(
    item: EmojiPickerItem,
    skinToneFactory: (EmojiPickerItem.UnicodeEmoji) -> String,
    onClick: (EmojiPickerItem) -> Unit,
    onServerEmoteInfo: (String) -> Unit,
    lesserHeaders: Boolean = false
) {
    when (item) {
        is EmojiPickerItem.UnicodeEmoji -> {
            Column(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        onClick(item)
                    }
                    .aspectRatio(1f)
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = skinToneFactory(item),
                    style = LocalTextStyle.current.copy(fontSize = 20.sp)
                )
            }
        }

        is EmojiPickerItem.ServerEmote -> {
            Column(
                modifier = Modifier
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = { onClick(item) },
                        onLongClick = { item.emote.id?.let { onServerEmoteInfo(it) } }
                    )
                    .aspectRatio(1f)
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RemoteImage(
                    url = "$REVOLT_FILES/emojis/${item.emote.id}",
                    description = item.emote.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
        }

        is EmojiPickerItem.Section -> {
            Text(
                when (item.category) {
                    is Category.UnicodeEmojiCategory -> stringResource(
                        item.category.definition.nameResource
                    )

                    is Category.ServerEmoteCategory ->
                        item.category.server.name
                            ?: stringResource(R.string.unknown)
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .then(
                        if (lesserHeaders) {
                            Modifier.alpha(.7f)
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}
