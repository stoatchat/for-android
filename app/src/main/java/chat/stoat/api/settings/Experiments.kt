package chat.stoat.api.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.stoat.BuildConfig
import chat.stoat.StoatApplication
import chat.stoat.persistence.KVStorage

class ExperimentInstance(default: Boolean) {
    private var _isEnabled by mutableStateOf(default)
    val isEnabled: Boolean
        get() = LoadedSettings.experimentsEnabled && _isEnabled

    fun setEnabled(enabled: Boolean) {
        _isEnabled = enabled
    }
}

/**
 * Experiments are boolean feature flags that can be toggled by the user in a self-service manner.
 * Unlike regular feature flags they are created with the goal of going live in the future.
 * They come with multiple safeguards:
 *  - Users must first enable experiments in the settings by performing a hidden action. They are then warned about potential instability.
 *  - Experiment states are not persisted across devices or uninstalls.
 *  - All experiments can be disabled at once with a single toggle.
 */
object Experiments {
    val usePolar = ExperimentInstance(false)
    val enableServerIdentityOptions = ExperimentInstance(false)

    suspend fun hydrateWithKv() {
        val kvStorage = KVStorage(StoatApplication.instance)

        if (BuildConfig.DEBUG) {
            LoadedSettings.experimentsEnabled = true
        } else {
            LoadedSettings.experimentsEnabled = kvStorage.getBoolean("experimentsEnabled") == true
        }

        usePolar.setEnabled(
            kvStorage.getBoolean("exp/usePolar") == true
        )
        enableServerIdentityOptions.setEnabled(
            kvStorage.getBoolean("exp/enableServerIdentityOptions") == true
        )
    }
}