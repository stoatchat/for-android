package chat.stoat

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.StrictMode
import chat.stoat.di.appModule
import chat.stoat.di.viewModelModule
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.google.android.material.color.DynamicColors
import io.livekit.android.LiveKit
import io.livekit.android.util.LoggingLevel
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class StoatApplication : Application(), SingletonImageLoader.Factory {
    companion object {
        lateinit var instance: StoatApplication
    }

    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)

        if (BuildConfig.DEBUG) {
            LiveKit.loggingLevel = LoggingLevel.DEBUG
        }

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

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }

    init {
        instance = this
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
