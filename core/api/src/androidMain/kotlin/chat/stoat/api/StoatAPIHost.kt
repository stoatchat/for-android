package chat.stoat.api

import app.cash.sqldelight.db.SqlDriver
import chat.stoat.api.settings.Theme
import chat.stoat.api.settings.UserInterfaceFont
import chat.stoat.persistence.KVStorage
import okhttp3.Interceptor

data class StoatAPIConfig(
    val versionName: String,
    val applicationId: String,
    val isDebug: Boolean,
)

interface StoatAPIStorage {
    val sqlDriver: SqlDriver
    val kvStorage: KVStorage
}

interface StoatAPIPlatform {
    val defaultTheme: Theme
    val defaultFont: UserInterfaceFont
    val httpInterceptors: List<Interceptor>

    fun onRealtimeHydrated()
}

// Stands in until the app installs its own, so Compose previews and tests can touch
// LoadedSettings and StoatHttp without a running StoatApplication.
private object FallbackStoatAPIPlatform : StoatAPIPlatform {
    override val defaultTheme = Theme.Default
    override val defaultFont = UserInterfaceFont.Default
    override val httpInterceptors = emptyList<Interceptor>()

    override fun onRealtimeHydrated() {}
}

// Must be installed by the app before any API call is made.
object StoatAPIHost {
    lateinit var config: StoatAPIConfig
        private set
    lateinit var storage: StoatAPIStorage
        private set
    var platform: StoatAPIPlatform = FallbackStoatAPIPlatform
        private set

    fun install(config: StoatAPIConfig, storage: StoatAPIStorage, platform: StoatAPIPlatform) {
        this.config = config
        this.storage = storage
        this.platform = platform
    }
}
