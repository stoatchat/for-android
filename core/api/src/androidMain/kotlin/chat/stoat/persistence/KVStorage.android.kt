package chat.stoat.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.stoatKVStorage: DataStore<Preferences> by preferencesDataStore(name = "revolt_kv")

fun KVStorage(context: Context): KVStorage = KVStorage(context.stoatKVStorage)
