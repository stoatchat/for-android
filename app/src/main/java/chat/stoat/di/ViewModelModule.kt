package chat.stoat.di

import chat.stoat.activities.MainActivityViewModel
import chat.stoat.activities.ShareTargetScreenViewModel
import chat.stoat.screens.chat.ChatRouterViewModel
import chat.stoat.screens.chat.views.channel.ChannelScreenViewModel
import chat.stoat.screens.login.LoginViewModel
import chat.stoat.screens.login.MfaScreenViewModel
import chat.stoat.screens.settings.AccountSettingsScreenViewModel
import chat.stoat.screens.settings.AppearanceSettingsScreenViewModel
import chat.stoat.screens.settings.DebugSettingsScreenViewModel
import chat.stoat.screens.settings.MfaSettingsScreenViewModel
import chat.stoat.screens.settings.NotificationsSettingsScreenViewModel
import chat.stoat.screens.settings.ProfileSettingsScreenViewModel
import chat.stoat.screens.settings.SettingsScreenViewModel
import chat.stoat.screens.settings.channel.ChannelSettingsOverviewViewModel
import chat.stoat.sheets.MemberListSheetViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainActivityViewModel(get(), androidContext()) }
    viewModel { ChatRouterViewModel(get(), androidContext()) }
    viewModel { MemberListSheetViewModel(androidApplication()) }
    viewModel { ShareTargetScreenViewModel(get()) }
    viewModel { ChannelScreenViewModel(get()) }
    viewModel { MfaScreenViewModel(get()) }
    viewModel { SettingsScreenViewModel(get()) }
    viewModel { DebugSettingsScreenViewModel(get()) }
    viewModel { NotificationsSettingsScreenViewModel(get(), androidContext()) }
    viewModel { LoginViewModel(get()) }
    viewModel { ProfileSettingsScreenViewModel(androidApplication()) }
    viewModel { AppearanceSettingsScreenViewModel(androidApplication()) }
    viewModel { ChannelSettingsOverviewViewModel(androidApplication()) }
    viewModel { AccountSettingsScreenViewModel(androidApplication()) }
    viewModel { MfaSettingsScreenViewModel(androidApplication()) }
}