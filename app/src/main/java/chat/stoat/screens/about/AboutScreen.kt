package chat.stoat.screens.about

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.stoat.BuildConfig
import chat.stoat.R
import chat.stoat.api.StoatJson
import chat.stoat.api.routes.misc.getRootRoute
import chat.stoat.core.model.data.STOAT_BASE
import chat.stoat.internals.Platform
import chat.stoat.settings.dsl.SettingsPage
import chat.stoat.ui.theme.FragmentMono
import kotlinx.coroutines.launch
import java.net.URI

class AboutViewModel : ViewModel() {
    var debugInfo by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    init {
        viewModelScope.launch {
            runCatching { getRootRoute() }.getOrNull()?.let {
                debugInfo = mapOf(
                    "App Version" to BuildConfig.VERSION_NAME,
                    "App Type" to BuildConfig.FLAVOUR_ID,
                    "API Host" to URI(STOAT_BASE).host,
                    "API Version" to it.revolt,
                    "Runtime SDK" to Build.VERSION.SDK_INT.toString(),
                    "Model" to "${Build.MANUFACTURER} ${
                        Build.DEVICE.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        }
                    } (${Build.MODEL})"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController, viewModel: AboutViewModel = viewModel()) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    var debugInfoOpen by remember { mutableStateOf(false) }
    val debugInfoChevronRotation by animateFloatAsState(if (debugInfoOpen) 270f else 90f)

    SettingsPage(
        navController,
        title = {
            Text(
                text = stringResource(id = R.string.about),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.stoat_logo_white),
                    contentDescription = stringResource(R.string.about_full_name),
                    colorFilter = ColorFilter.tint(LocalContentColor.current),
                    modifier = Modifier
                        .width(250.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.about_full_name),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clickable { debugInfoOpen = !debugInfoOpen }
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.7f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.about_section_debug_information),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.about_section_debug_information_description),
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = LocalContentColor.current.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_forward_24dp),
                        contentDescription = null,
                        tint = LocalContentColor.current.copy(alpha = 0.5f),
                        modifier = Modifier.rotate(debugInfoChevronRotation)
                    )
                }

                AnimatedVisibility(visible = debugInfoOpen) {
                    Column(Modifier.padding(horizontal = 8.dp)) {
                        Spacer(Modifier.height(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            viewModel.debugInfo.forEach { (key, value) ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 0.05.em,
                                            textAlign = TextAlign.Start
                                        )
                                    )

                                    Spacer(Modifier.weight(1f))

                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Light,
                                            fontFamily = FragmentMono,
                                            textAlign = TextAlign.End
                                        ),
                                        color = LocalContentColor.current.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            ClipData.newPlainText(
                                                "Stoat Debug Information",
                                                StoatJson.encodeToString(viewModel.debugInfo)
                                            ).toClipEntry()
                                        )

                                        if (Platform.needsShowClipboardNotification()) {
                                            Toast.makeText(
                                                context,
                                                resources.getString(R.string.copied),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_content_copy_24dp),
                                    contentDescription = null
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(text = stringResource(id = R.string.about_section_debug_information_copy_as_json))
                            }

                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clickable { navController.navigate("about/oss") }
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.7f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.about_section_oss_licenses),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_forward_24dp),
                        contentDescription = null,
                        tint = LocalContentColor.current.copy(alpha = 0.5f)
                    )
                }
            }

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
    }
}
