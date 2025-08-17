package chat.peptide.screens.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.peptide.BuildConfig
import chat.peptide.PeptideApplication
import chat.peptide.api.settings.Experiments
import chat.peptide.api.settings.LoadedSettings
import chat.peptide.persistence.KVStorage
import chat.peptide.settings.dsl.SettingsPage
import chat.peptide.settings.dsl.SubcategoryContentInsets
import kotlinx.coroutines.launch

class ExperimentsSettingsScreenViewModel : ViewModel() {
    private val kv = KVStorage(PeptideApplication.instance)

    fun init() {
        viewModelScope.launch {
            useKotlinMdRendererChecked.value = Experiments.useKotlinBasedMarkdownRenderer.isEnabled
            usePolarChecked.value = Experiments.usePolar.isEnabled
            enableServerIdentityOptionsChecked.value =
                Experiments.enableServerIdentityOptions.isEnabled
        }
    }

    val showNeedsRestartAlert = mutableStateOf(false)

    // cf. https://stackoverflow.com/a/46848226
    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent == null) {
            return
        }

        val componentName = intent.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        mainIntent.`package` = context.packageName
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    fun disableExperiments(then: () -> Unit = {}) {
        viewModelScope.launch {
            kv.remove("experimentsEnabled")
            LoadedSettings.experimentsEnabled = false
            then()
        }
    }

    val useKotlinMdRendererChecked = mutableStateOf(false)

    fun setUseKotlinMdRendererChecked(value: Boolean) {
        viewModelScope.launch {
            kv.set("exp/useKotlinBasedMarkdownRenderer", value)
            Experiments.useKotlinBasedMarkdownRenderer.setEnabled(value)
            useKotlinMdRendererChecked.value = value
        }
    }

    val usePolarChecked = mutableStateOf(false)

    fun setUsePolarChecked(value: Boolean) {
        viewModelScope.launch {
            kv.set("exp/usePolar", value)
            Experiments.usePolar.setEnabled(value)
            showNeedsRestartAlert.value = true
            usePolarChecked.value = value
        }
    }

    val enableServerIdentityOptionsChecked = mutableStateOf(false)

    fun setEnableServerIdentityOptionsChecked(value: Boolean) {
        viewModelScope.launch {
            kv.set("exp/enableServerIdentityOptions", value)
            Experiments.enableServerIdentityOptions.setEnabled(value)
            enableServerIdentityOptionsChecked.value = value
        }
    }
}

@Composable
fun ExperimentsSettingsScreen(
    navController: NavController,
    viewModel: ExperimentsSettingsScreenViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.init()
    }

    if (viewModel.showNeedsRestartAlert.value) {
        AlertDialog(
            onDismissRequest = {
                viewModel.showNeedsRestartAlert.value = false
            },
            title = {
                Text("Restart Required")
            },
            text = {
                Text("The changes you made require a restart to take effect.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restartApp(context)
                    }
                ) {
                    Text("Restart")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.showNeedsRestartAlert.value = false
                    }
                ) {
                    Text("Later")
                }
            }
        )
    }

    SettingsPage(
        navController,
        title = {
            Text("Experiments", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    ) {
        ListItem(
            headlineContent = {
                Text("New Message Markdown Renderer")
            },
            supportingContent = {
                Text("Use a Kotlin-based Markdown renderer for messages rather than the C++ one. Missing features may be present.")
            },
            trailingContent = {
                Switch(
                    checked = viewModel.useKotlinMdRendererChecked.value,
                    onCheckedChange = null
                )
            },
            modifier = Modifier.clickable { viewModel.setUseKotlinMdRendererChecked(!viewModel.useKotlinMdRendererChecked.value) }
        )

        ListItem(
            headlineContent = {
                Text("Threefold Root User Interface")
            },
            supportingContent = {
                Text("Polar")
            },
            trailingContent = {
                Switch(
                    checked = viewModel.usePolarChecked.value,
                    onCheckedChange = null
                )
            },
            modifier = Modifier.clickable { viewModel.setUsePolarChecked(!viewModel.usePolarChecked.value) }
        )

        ListItem(
            headlineContent = {
                Text("Server Identity Options")
            },
            supportingContent = {
                Text("Enable options to control what parts of others' server identities you want to see.")
            },
            trailingContent = {
                Switch(
                    checked = viewModel.enableServerIdentityOptionsChecked.value,
                    onCheckedChange = null
                )
            },
            modifier = Modifier.clickable { viewModel.setEnableServerIdentityOptionsChecked(!viewModel.enableServerIdentityOptionsChecked.value) }
        )

        Subcategory(
            title = {
                Text("Disable experiments")
            },
            contentInsets = SubcategoryContentInsets
        ) {
            ElevatedButton(
                onClick = {
                    viewModel.disableExperiments {
                        navController.popBackStack()
                    }
                },
                enabled = !BuildConfig.DEBUG,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (BuildConfig.DEBUG) {
                    Text("Experiments are always enabled in debug builds")
                } else {
                    Text("Disable")
                }
            }
        }
    }
}