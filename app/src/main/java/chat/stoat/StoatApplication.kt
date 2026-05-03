package chat.stoat

import android.app.Application
import android.os.Build
import android.os.StrictMode
import chat.stoat.di.appModule
import chat.stoat.di.viewModelModule
import com.google.android.material.color.DynamicColors
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class StoatApplication : Application() {
    companion object {
        lateinit var instance: StoatApplication
    }

    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)

        startKoin {
            androidContext(this@StoatApplication)
            androidLogger()
            modules(appModule, viewModelModule)
        }

        if (BuildConfig.DEBUG) {
            // Enable strict mode primarily to catch non-API usage, although we detect all
            // violations for our reference.
            // https://developer.android.com/reference/android/os/StrictMode
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy
                    .Builder()
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            detectNonSdkApiUsage()
                        }
                        penaltyLog()
                    }
                    .build()
            )
        }
    }

    init {
        instance = this
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
