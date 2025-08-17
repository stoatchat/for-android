package chat.peptide

import android.app.Application
import chat.peptide.api.PeptideHttp
import chat.peptide.persistence.KVStorage
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import logcat.AndroidLogcatLogger
import logcat.LogPriority

@HiltAndroidApp
class PeptideApplication : Application() {
    companion object {
        lateinit var instance: PeptideApplication
    }

    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)
        PeptideHttp // Trigger initialization
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
fun PeptideApplication.getKVStorage(): KVStorage {
    return KVStorage(this)
}
