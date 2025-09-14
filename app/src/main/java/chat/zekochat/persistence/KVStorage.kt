package chat.zekochat.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

val Context.peptideKVStorage: DataStore<Preferences> by preferencesDataStore(name = "peptide_kv")

@Singleton
class KVStorage @Inject constructor(
    @ApplicationContext private val mContext: Context
) {
    private val dataStore = mContext.peptideKVStorage

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
