package chat.stoat.screens.settings

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatCbor
import chat.stoat.api.StoatJson
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.api.settings.SyncedSettings
import chat.stoat.api.settings.UserInterfaceFont
import chat.stoat.composables.generic.ListHeader
import chat.stoat.composables.screens.settings.appearance.ColourChip
import chat.stoat.composables.screens.settings.appearance.CornerRadiusPicker
import chat.stoat.core.model.data.OverridableColourScheme
import chat.stoat.sheets.ColourPickerSheet
import chat.stoat.ui.theme.GoogleSansFlex
import chat.stoat.ui.theme.Inter
import chat.stoat.ui.theme.Theme
import chat.stoat.ui.theme.applyFromKeyValueMap
import chat.stoat.ui.theme.getFieldByName
import chat.stoat.ui.theme.overridableColourSchemeFieldNameToResource
import chat.stoat.ui.theme.overridableColourSchemeFieldNames
import chat.stoat.ui.theme.systemSupportsDynamicColors
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.koin.androidx.compose.koinViewModel
import java.io.File

class AppearanceSettingsScreenViewModel(val context: Application) : ViewModel() {
    var showColourOverrides by mutableStateOf(false)
    var selectedOverrideName by mutableStateOf<String?>(null)
    var selectedOverrideInitialValue by mutableStateOf<Int?>(null)
    var overridePickerSheetVisible by mutableStateOf(false)

    fun saveNewTheme(theme: Theme) {
        LoadedSettings.theme = theme
        viewModelScope.launch {
            SyncedSettings.updateAndroid(SyncedSettings.android.copy(theme = theme.name))
        }
    }

    fun saveNewFont(font: UserInterfaceFont) {
        LoadedSettings.font = font
        viewModelScope.launch {
            SyncedSettings.updateAndroid(SyncedSettings.android.copy(font = font.name))
        }
    }

    fun saveNewAvatarRadius(radius: Int) {
        LoadedSettings.avatarRadius = radius
        viewModelScope.launch {
            SyncedSettings.updateAndroid(SyncedSettings.android.copy(avatarRadius = radius))
        }
    }

    fun updateColourOverrides(fieldName: String, value: Int?) {
        viewModelScope.launch {
            val overrides = SyncedSettings.android.copy().colourOverrides

            if (overrides != null) {
                // Yes, this looks stupid. Please see the comments in OverridableColourScheme.kt regarding this.
                val json = StoatJson.encodeToString(
                    OverridableColourScheme.serializer(),
                    overrides
                )
                val asMap = StoatJson.decodeFromString(
                    MapSerializer(String.serializer(), Int.serializer()),
                    json
                )

                val mutOverrides = asMap.toMutableMap()
                if (value == null) {
                    mutOverrides.remove(fieldName)
                } else {
                    mutOverrides[fieldName] = value
                }

                SyncedSettings.updateAndroid(
                    SyncedSettings.android.copy(
                        colourOverrides = OverridableColourScheme()
                            .applyFromKeyValueMap(mutOverrides)
                    )
                )
            } else if (value != null) {
                SyncedSettings.updateAndroid(
                    SyncedSettings.android.copy(
                        colourOverrides = OverridableColourScheme()
                            .applyFromKeyValueMap(
                                mapOf(fieldName to value)
                            )
                    )
                )
            }
        }
    }

    private fun applyBulkOverrides(overrides: Map<String, Int>) {
        val existingOverrides = SyncedSettings.android.colourOverrides ?: OverridableColourScheme()

        viewModelScope.launch {
            SyncedSettings.updateAndroid(
                SyncedSettings.android.copy(
                    colourOverrides = existingOverrides
                        .applyFromKeyValueMap(overrides.filterKeys { it in overridableColourSchemeFieldNames })
                )
            )
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun processImportedOverrides(uri: Uri) {
        val mFile = File(context.cacheDir, uri.lastPathSegment ?: "temp")

        mFile.outputStream().use { outputStream ->
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        try {
            StoatCbor.decodeFromByteArray(
                MapSerializer(String.serializer(), Int.serializer()),
                mFile.readBytes()
            ).let {
                applyBulkOverrides(it)
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.settings_appearance_colour_overrides_import_error),
                Toast.LENGTH_SHORT
            ).show()
        }

        mFile.delete()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun saveOverridesToFile(uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(
                StoatCbor.encodeToByteArray(
                    OverridableColourScheme.serializer(),
                    SyncedSettings.android.colourOverrides ?: OverridableColourScheme()
                )
            )
        }
    }
}

@OptIn(
    ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun AppearanceSettingsScreen(
    navController: NavController,
    viewModel: AppearanceSettingsScreenViewModel = koinViewModel()
) {
    val colourOverridesOpenerArrowRotation by animateFloatAsState(
        if (viewModel.showColourOverrides) {
            if (LocalLayoutDirection.current == LayoutDirection.Ltr) 90f else -90f
        } else 0f,
        label = "colourOverridesOpenerArrowRotation"
    )

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.processImportedOverrides(uri)
        }
    }
    val fileSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-stoat-android-theme-overrides"),
    ) { uri ->
        if (uri != null) {
            viewModel.saveOverridesToFile(uri)
        }
    }

    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    if (viewModel.overridePickerSheetVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                viewModel.overridePickerSheetVisible = false
            }
        ) {
            ColourPickerSheet(
                initialValue = viewModel.selectedOverrideInitialValue ?: 0,
                onColourSelected = {
                    viewModel.updateColourOverrides(
                        viewModel.selectedOverrideName ?: return@ColourPickerSheet,
                        it
                    )
                    scope.launch {
                        sheetState.hide()
                        viewModel.overridePickerSheetVisible = false
                    }
                },
                onUseDefaultColour = {
                    viewModel.updateColourOverrides(
                        viewModel.selectedOverrideName ?: return@ColourPickerSheet,
                        null
                    )
                    scope.launch {
                        sheetState.hide()
                        viewModel.overridePickerSheetVisible = false
                    }
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        viewModel.overridePickerSheetVisible = false
                    }
                }
            )
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.settings_appearance),
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
                ListHeader {
                    Text(stringResource(R.string.settings_appearance_theme))
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp)
                ) {
                    ColourChip(
                        color = Color(0xff333642),
                        text = stringResource(id = R.string.settings_appearance_theme_default),
                        selected = LoadedSettings.theme == Theme.Default,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_theme_default")
                    ) {
                        viewModel.saveNewTheme(Theme.Default)
                    }

                    ColourChip(
                        color = Color(0xfff7f7f7),
                        text = stringResource(id = R.string.settings_appearance_theme_light),
                        selected = LoadedSettings.theme == Theme.Light,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_theme_light")
                    ) {
                        viewModel.saveNewTheme(Theme.Light)
                    }

                    ColourChip(
                        color = Color(0xff000000),
                        text = stringResource(id = R.string.settings_appearance_theme_amoled),
                        selected = LoadedSettings.theme == Theme.Amoled,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_theme_amoled")
                    ) {
                        viewModel.saveNewTheme(Theme.Amoled)
                    }

                    ColourChip(
                        color = if (isSystemInDarkTheme()) Color(0xff333642) else Color(0xfff7f7f7),
                        text = stringResource(id = R.string.settings_appearance_theme_none),
                        selected = LoadedSettings.theme == Theme.None,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_theme_none")
                    ) {
                        viewModel.saveNewTheme(Theme.None)
                    }

                    if (systemSupportsDynamicColors()) {
                        ColourChip(
                            color = dynamicDarkColorScheme(LocalContext.current).primary,
                            text = stringResource(id = R.string.settings_appearance_theme_m3dynamic),
                            selected = LoadedSettings.theme == Theme.M3Dynamic,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("set_theme_m3dynamic")
                        ) {
                            viewModel.saveNewTheme(Theme.M3Dynamic)
                        }
                    } else {
                        ColourChip(
                            color = Color(0xffa0a0a0),
                            text = stringResource(
                                id = R.string.settings_appearance_theme_m3dynamic_unsupported
                            ),
                            selected = false,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("set_theme_m3dynamic_unsupported")
                        ) {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.settings_appearance_theme_m3dynamic_unsupported_toast),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                ListHeader {
                    Text(stringResource(R.string.settings_appearance_typeface))
                }

                FlowRow(
                    Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ToggleButton(
                        checked = LoadedSettings.font == UserInterfaceFont.Default,
                        onCheckedChange = { viewModel.saveNewFont(UserInterfaceFont.Default) },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        modifier = Modifier
                            .semantics { role = Role.RadioButton }
                            .weight(1f),
                    ) {
                        Text(
                            stringResource(R.string.settings_appearance_typeface_default),
                            fontFamily = Inter
                        )
                    }
                    ToggleButton(
                        checked = LoadedSettings.font == UserInterfaceFont.GoogleSansFlex,
                        onCheckedChange = { viewModel.saveNewFont(UserInterfaceFont.GoogleSansFlex) },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier
                            .semantics { role = Role.RadioButton }
                            .weight(1f),
                    ) {
                        Text(
                            stringResource(R.string.settings_appearance_typeface_google_sans_flex),
                            fontFamily = GoogleSansFlex
                        )
                    }
                }

                ListHeader {
                    Text(stringResource(R.string.settings_appearance_avatar_shape))
                }

                Text(
                    stringResource(R.string.settings_appearance_avatar_shape_description),
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                )

                Box(Modifier.padding(horizontal = 16.dp)) {
                    CornerRadiusPicker(
                        percentage = LoadedSettings.avatarRadius,
                        onUpdate = {
                            viewModel.saveNewAvatarRadius(it)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .clickable {
                            viewModel.showColourOverrides = !viewModel.showColourOverrides
                        }
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_keyboard_arrow_right_24dp),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 20.dp, end = 4.dp)
                            .rotate(colourOverridesOpenerArrowRotation)
                    )

                    Text(
                        text = stringResource(id = R.string.settings_appearance_colour_overrides),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .weight(1f)
                    )
                }

                AnimatedVisibility(viewModel.showColourOverrides) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    filePicker.launch(arrayOf("*/*"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_file_open_24dp),
                                    contentDescription = null
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = stringResource(id = R.string.settings_appearance_colour_overrides_import)
                                )
                            }

                            TextButton(
                                onClick = {
                                    fileSaver.launch("${SyncedSettings.android.theme}-colours.rato")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_file_export_24dp),
                                    contentDescription = null
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = stringResource(id = R.string.settings_appearance_colour_overrides_export)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        overridableColourSchemeFieldNames.forEach { fieldName ->
                            val value =
                                SyncedSettings.android.colourOverrides?.getFieldByName(fieldName)
                                    ?: MaterialTheme.colorScheme.getFieldByName(fieldName)

                            ColourChip(
                                color = Color(value ?: 0),
                                text = overridableColourSchemeFieldNameToResource[fieldName]
                                    ?.let { stringResource(it) }
                                    ?: fieldName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 20.dp)
                                    .testTag("set_colour_override_$fieldName")
                            ) {
                                viewModel.selectedOverrideName = fieldName
                                viewModel.selectedOverrideInitialValue = value
                                viewModel.overridePickerSheetVisible = true
                            }
                        }
                    }
                }
            }
        }
    }
}