package chat.stoat.di

import chat.stoat.composables.markdown.prose.ChatMarkdownActions
import chat.stoat.internals.AppChatMarkdownActions
import chat.stoat.persistence.KVStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

//
val appModule = module {
    single { KVStorage(androidContext()) }
    single<ChatMarkdownActions> { AppChatMarkdownActions(androidContext()) }
}