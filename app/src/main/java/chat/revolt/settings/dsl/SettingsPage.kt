package chat.revolt.settings.dsl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.composables.generic.ListHeader
import chat.revolt.composables.generic.RadioItem
import kotlinx.coroutines.launch
import kotlin.enums.EnumEntries

val SubcategoryContentInsets = PaddingValues(horizontal = 16.dp)

interface SettingsPageScope {
    fun showSnackbar(message: String)

    @Composable
    fun Subcategory(
        title: @Composable () -> Unit,
        contentInsets: PaddingValues,
        content: @Composable SettingsPageScope.() -> Unit
    ) {
        ListHeader {
            title()
        }

        Column(
            Modifier
                .padding(contentInsets)
        ) {
            content()
        }
    }

    @Composable
    fun Subcategory(
        title: @Composable () -> Unit,
        content: @Composable SettingsPageScope.() -> Unit
    ) = Subcategory(
        title = title,
        contentInsets = PaddingValues(0.dp),
        content = content
    )

    @Composable
    fun <X : Enum<X>> RadioOptions(
        options: EnumEntries<X>,
        selectedOption: X,
        onOptionSelected: (X) -> Unit
    ) {
        Column(Modifier.selectableGroup()) {
            for (option in options) {
                RadioItem(
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) },
                    label = { Text(option.toString()) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    navController: NavController?,
    title: @Composable () -> Unit,
    content: @Composable SettingsPageScope.() -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = title,
                navigationIcon = {
                    navController?.let {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.icn_arrow_back_24dp),
                                contentDescription = stringResource(id = R.string.back)
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { pv ->
        Column(
            Modifier
                .padding(pv)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            content(
                object : SettingsPageScope {
                    override fun showSnackbar(message: String) {
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                }
            )
        }
    }
}