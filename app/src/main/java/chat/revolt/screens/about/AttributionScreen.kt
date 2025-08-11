package chat.revolt.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.composables.screens.settings.AttributionItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

@Serializable
data class AboutLibraries(
    val metadata: Metadata,
    val libraries: List<Library>,
    val licenses: Map<String, License>
)

@Serializable
data class Metadata(
    val generated: String
)

@Serializable
data class License(
    val content: String? = null,
    val hash: String,
    val internalHash: String? = null,
    val url: String,
    val spdxId: String? = null,
    val name: String
)

@Serializable
data class Library(
    val uniqueId: String,
    val funding: JsonArray,
    val developers: List<Developer>,
    val artifactVersion: String,
    val description: String,
    val scm: Scm? = null,
    val name: String,
    val licenses: List<String>,
    val website: String? = null,
    val organization: Organization? = null
)

@Serializable
data class Organization(
    val url: String? = null,
    val name: String? = null
)

@Serializable
data class Developer(
    val organisationUrl: String? = null,
    val name: String? = null
)

@Serializable
data class Scm(
    val connection: String? = null,
    val url: String,
    val developerConnection: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributionScreen(navController: NavController) {
    var libraries by remember { mutableStateOf<AboutLibraries?>(null) }

    val context = LocalContext.current

    var licenceSheetOpen by remember { mutableStateOf(false) }
    var licenseSheetTarget by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        context.resources.openRawResource(R.raw.aboutlibraries).use { stream ->
            val text = stream.bufferedReader().use { it.readText() }
            libraries = Json.decodeFromString(AboutLibraries.serializer(), text)
        }
    }

    if (licenceSheetOpen) {
        val licenceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = licenceSheetState,
            onDismissRequest = {
                licenceSheetOpen = false
            }
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = licenseSheetTarget,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                libraries?.let {
                    val license = it.licenses[licenseSheetTarget]
                    if (license != null) {
                        Text(text = license.content ?: "No license content found.")
                    } else {
                        Text(text = "No license found.")
                    }
                }
            }

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
                        text = stringResource(R.string.oss_attribution),
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
                },
            )
        },
    ) { pv ->
        Box(Modifier.padding(pv)) {
            libraries?.let {
                LazyColumn {
                    item {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.oss_attribution_body)
                            )
                            Text(
                                text = stringResource(R.string.oss_attribution_body_2)
                            )
                            Text(
                                text = stringResource(R.string.oss_attribution_warning),
                                color = MaterialTheme.colorScheme.error
                            )

                            Text(
                                text = stringResource(
                                    R.string.oss_attribution_generation_date,
                                    libraries?.metadata?.generated ?: ""
                                ),
                                color = LocalContentColor.current.copy(alpha = 0.6f)
                            )
                        }
                    }

                    items(
                        items = it.libraries.sortedBy { library -> library.name }
                    ) { library ->
                        AttributionItem(library = library) {
                            licenceSheetOpen = true
                            licenseSheetTarget = library.licenses.firstOrNull() ?: ""
                        }
                    }

                    item(key = "cat") {
                        Text(
                            text = "🐈",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
