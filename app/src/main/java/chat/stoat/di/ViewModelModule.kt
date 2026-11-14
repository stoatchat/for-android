package chat.stoat.di

import chat.stoat.activities.InviteViewModel
import chat.stoat.activities.MainActivityViewModel
import chat.stoat.activities.ShareTargetScreenViewModel
import chat.stoat.screens.about.AboutViewModel
import chat.stoat.screens.changelogs.ReadChangelogScreenViewModel
import chat.stoat.screens.chat.ChannelPinsScreenViewModel
import chat.stoat.screens.chat.ChannelSearchScreenViewModel
import chat.stoat.screens.chat.ChatRouterViewModel
import chat.stoat.screens.chat.standalone.CatchUpScreenViewModel
import chat.stoat.screens.chat.views.channel.ChannelScreenViewModel
import chat.stoat.screens.create.CreateGroupScreenViewModel
import chat.stoat.screens.login.LoginViewModel
import chat.stoat.screens.login.MfaScreenViewModel
import chat.stoat.screens.register.RegisterDetailsScreenViewModel
import chat.stoat.screens.settings.AccountSettingsScreenViewModel
import chat.stoat.screens.settings.AppearanceSettingsScreenViewModel
import chat.stoat.screens.settings.ChatSettingsScreenViewModel
import chat.stoat.screens.settings.DebugSettingsScreenViewModel
import chat.stoat.screens.settings.ExperimentsSettingsScreenViewModel
import chat.stoat.screens.settings.LanguagePickerSettingsScreen
import chat.stoat.screens.settings.MfaSettingsScreenViewModel
import chat.stoat.screens.settings.NotificationsSettingsScreenViewModel
import chat.stoat.screens.settings.ProfileSettingsScreenViewModel
import chat.stoat.screens.settings.SessionSettingsScreenViewModel
import chat.stoat.screens.settings.SettingsScreenViewModel
import chat.stoat.screens.settings.channel.ChannelSettingsOverviewViewModel
import chat.stoat.screens.settings.channel.ChannelSettingsPermissionsViewModel
import chat.stoat.screens.settings.server.ServerIdentitySettingsViewModel
import chat.stoat.screens.settings.server.ServerSettingsBansViewModel
import chat.stoat.screens.settings.server.ServerSettingsChannelsViewModel
import chat.stoat.screens.settings.server.ServerSettingsEmojisViewModel
import chat.stoat.screens.settings.server.ServerSettingsInvitesViewModel
import chat.stoat.screens.settings.server.ServerSettingsOverviewViewModel
import chat.stoat.screens.settings.server.ServerSettingsRoleEditorViewModel
import chat.stoat.screens.settings.server.ServerSettingsRolesViewModel
import chat.stoat.sheets.MemberListSheetViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
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
    viewModel { ChannelSettingsPermissionsViewModel(androidApplication()) }
    viewModel { ServerSettingsOverviewViewModel(androidApplication()) }
    viewModel { ServerIdentitySettingsViewModel(androidApplication()) }
    viewModel { ServerSettingsBansViewModel(androidApplication()) }
    viewModel { ServerSettingsChannelsViewModel(androidApplication()) }
    viewModel { ServerSettingsEmojisViewModel(androidApplication()) }
    viewModel { ServerSettingsInvitesViewModel(androidApplication()) }
    viewModel { ServerSettingsRolesViewModel(androidApplication()) }
    viewModel { ServerSettingsRoleEditorViewModel(androidApplication()) }
    viewModel { AccountSettingsScreenViewModel(androidApplication()) }
    viewModel { MfaSettingsScreenViewModel(androidApplication()) }
    viewModel { (id: String) -> ReadChangelogScreenViewModel(id) }
    viewModelOf(::AboutViewModel)
    viewModelOf(::CatchUpScreenViewModel)
    viewModelOf(::ChannelPinsScreenViewModel)
    viewModelOf(::ChannelSearchScreenViewModel)
    viewModelOf(::ChatSettingsScreenViewModel)
    viewModelOf(::CreateGroupScreenViewModel)
    viewModelOf(::ExperimentsSettingsScreenViewModel)
    viewModelOf(::InviteViewModel)
    viewModelOf(::LanguagePickerSettingsScreen)
    viewModelOf(::RegisterDetailsScreenViewModel)
    viewModelOf(::SessionSettingsScreenViewModel)
}
