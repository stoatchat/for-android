package chat.peptide

import android.app.Application
import chat.peptide.api.RevoltHttp
import chat.peptide.persistence.KVStorage
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import logcat.AndroidLogcatLogger
import logcat.LogPriority

@HiltAndroidApp
class RevoltApplication : Application() {
    companion object {
        lateinit var instance: RevoltApplication
    }

    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)
        RevoltHttp // Trigger initialization
    }

    init {
        instance = this
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}

/**
 * Extension function to get KVStorage instance.
 * @return KVStorage instance
 */
fun RevoltApplication.getKVStorage(): KVStorage {
    return KVStorage(this)
}
