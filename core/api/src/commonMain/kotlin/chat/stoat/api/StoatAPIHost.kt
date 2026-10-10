package chat.stoat.api

import app.cash.sqldelight.db.SqlDriver
import chat.stoat.api.internals.defaultHttpEngine
import chat.stoat.api.settings.Theme
import chat.stoat.api.settings.UserInterfaceFont
import chat.stoat.persistence.KVStorage
import io.ktor.client.engine.HttpClientEngine

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

    fun createHttpEngine(): HttpClientEngine

    fun onRealtimeHydrated()
}

// Stands in until the app installs its own, so Compose previews and tests can touch
// LoadedSettings and StoatHttp without a running StoatApplication.
private object FallbackStoatAPIPlatform : StoatAPIPlatform {
    override val defaultTheme = Theme.Default
    override val defaultFont = UserInterfaceFont.Default

    override fun createHttpEngine() = defaultHttpEngine()

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
