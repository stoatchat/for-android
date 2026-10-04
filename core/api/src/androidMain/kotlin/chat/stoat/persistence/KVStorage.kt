package chat.stoat.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull

val Context.stoatKVStorage: DataStore<Preferences> by preferencesDataStore(name = "revolt_kv")

class KVStorage(
    private val mContext: Context
) {
    private val dataStore = mContext.stoatKVStorage

    suspend fun set(key: String, value: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    suspend fun get(key: String): String? {
        return dataStore.data.firstOrNull()?.get(stringPreferencesKey(key))
    }

    suspend fun set(key: String, value: Boolean) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value.toString()
        }
    }

    suspend fun getBoolean(key: String): Boolean? {
        return dataStore.data.firstOrNull()?.get(stringPreferencesKey(key))?.toBoolean()
    }

    suspend fun set(key: String, value: Int) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value.toString()
        }
    }

    suspend fun getInt(key: String): Int? {
        return dataStore.data.firstOrNull()?.get(stringPreferencesKey(key))?.toInt()
    }

    suspend fun remove(key: String) {
        dataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey(key))
        }
    }
}
