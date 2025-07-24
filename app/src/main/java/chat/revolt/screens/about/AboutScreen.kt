package chat.revolt.screens.about

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.BuildConfig
import chat.revolt.R
import chat.revolt.api.REVOLT_BASE
import chat.revolt.api.RevoltJson
import chat.revolt.api.routes.misc.Root
import chat.revolt.api.routes.misc.getRootRoute
import chat.revolt.composables.generic.PrimaryTabs
import chat.revolt.internals.Platform
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.net.URI

class AboutViewModel : ViewModel() {
    var root by mutableStateOf<Root?>(null)
    var selectedTabIndex by mutableIntStateOf(0)

    fun getDebugInformation(): Map<String, String> {
        return mapOf(
            "App ID" to BuildConfig.APPLICATION_ID,
            "App Version" to BuildConfig.VERSION_NAME,
            "App Type" to BuildConfig.FLAVOUR_ID,
            "API Host" to URI(REVOLT_BASE).host,
            "API Version" to (root?.revolt ?: "Unknown"),
            "Runtime SDK" to Build.VERSION.SDK_INT.toString(),
            "Model" to "${Build.MANUFACTURER} ${
                Build.DEVICE.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            } (${Build.MODEL})"
        )
    }

    init {
        viewModelScope.launch {
            root = getRootRoute().copy()
        }
    }
}

@Composable
fun VersionItem(key: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier) {
        Text(
            text = key,
            color = MaterialTheme.colorScheme.onBackground.copy(
                alpha = 1.0f
            ),
            style = MaterialTheme.typography.titleMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .padding(horizontal = 2.5.dp, vertical = 2.5.dp)
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onBackground.copy(
                alpha = 0.9f
            ),
            style = MaterialTheme.typography.titleMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier
                .padding(horizontal = 2.5.dp, vertical = 2.5.dp)
        )
    }
}

@Composable
fun DebugInfo(viewModel: AboutViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        viewModel.getDebugInformation().forEach { (key, value) ->
            VersionItem(key, value)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController, viewModel: AboutViewModel = viewModel()) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager =
        LocalClipboardManager.current

    fun copyDebugInformation() {
        clipboardManager.setText(
            AnnotatedString(RevoltJson.encodeToString(viewModel.getDebugInformation()))
        )

        if (Platform.needsShowClipboardNotification()) {
            Toast.makeText(
                context,
                context.getString(R.string.copied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.about),
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
                }
            )
        },
    ) { pv ->
        Box(Modifier.padding(pv)) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrimaryTabs(
                    tabs = listOf(
                        stringResource(R.string.about_tab_version),
                        stringResource(R.string.about_tab_details)
                    ),
                    currentIndex = viewModel.selectedTabIndex,
                    onTabSelected = { viewModel.selectedTabIndex = it }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (viewModel.root == null) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(48.dp)
                        )
                    } else {
                        when (viewModel.selectedTabIndex) {
                            0 -> {
                                Image(
                                    painter = painterResource(R.drawable.revolt_logo_wide),
                                    contentDescription = stringResource(R.string.about_full_name),
                                    colorFilter = ColorFilter.tint(LocalContentColor.current),
                                    modifier = Modifier
                                        .width(250.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = stringResource(R.string.about_full_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = BuildConfig.VERSION_NAME,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Normal
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Text(
                                    text = stringResource(R.string.about_brought_to_you_by),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Light
                                    ),
                                    color = LocalContentColor.current.copy(
                                        alpha = 0.5f
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }

                            1 -> {
                                DebugInfo(viewModel)
                                TextButton(onClick = ::copyDebugInformation) {
                                    Text(text = stringResource(id = R.string.copy))
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ElevatedButton(
                        onClick = { navController.navigate("about/oss") },
                        modifier = Modifier
                            .testTag("view_oss_attribution")
                    ) {
                        Text(text = stringResource(id = R.string.oss_attribution))
                    }
                }
            }
        }
    }
}
