package chat.revolt.screens.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.internals.extensions.getSupportedLocales
import java.util.Locale

class LanguagePickerSettingsScreen : ViewModel() {
    val locales = mutableStateListOf<Locale>()

    var currentLocale by mutableStateOf<Locale?>(null)

    fun initLanguages(context: Context) {
        locales.clear()
        locales.addAll(context.getSupportedLocales())
        currentLocale = determineCurrentlySelectedLocale()
    }

    private fun determineCurrentlySelectedLocale(): Locale? {
        return AppCompatDelegate.getApplicationLocales()[0]
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSettingsScreen(
    navController: NavController,
    viewModel: LanguagePickerSettingsScreen = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initLanguages(context)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        stringResource(id = R.string.settings_language),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
    ) { pv ->
        LazyColumn(
            contentPadding = pv,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            item(key = "auto") {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(id = R.string.settings_language_auto),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingContent = {
                        RadioButton(
                            selected = viewModel.currentLocale == null,
                            onClick = null
                        )
                    },
                    modifier = Modifier.clickable {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                        viewModel.currentLocale = null
                    }
                )
            }
            items(
                viewModel.locales.size,
                key = { index -> viewModel.locales[index].toLanguageTag() }
            ) { index ->
                val locale = viewModel.locales[index]
                ListItem(
                    headlineContent = {
                        Text(
                            locale.displayLanguage,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = {
                        if (!locale.displayCountry.isNullOrEmpty()) {
                            Text(
                                locale.displayCountry,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    trailingContent = {
                        Text(
                            locale.getDisplayLanguage(locale),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingContent = {
                        RadioButton(
                            selected = locale.toLanguageTag() == viewModel.currentLocale?.toLanguageTag(),
                            onClick = null
                        )
                    },
                    modifier = Modifier
                        .clickable {
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(
                                    locale.toLanguageTag()
                                )
                            )
                            viewModel.currentLocale = locale
                        }
                )
            }
        }
    }
}