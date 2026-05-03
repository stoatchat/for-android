package chat.stoat.di

import chat.stoat.persistence.KVStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

//
val appModule = module {
    single { KVStorage(androidContext()) }
}