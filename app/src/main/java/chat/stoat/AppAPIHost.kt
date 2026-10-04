package chat.stoat

import app.cash.sqldelight.db.SqlDriver
import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatAPIConfig
import chat.stoat.api.StoatAPIHost
import chat.stoat.api.StoatAPIPlatform
import chat.stoat.api.StoatAPIStorage
import chat.stoat.api.routes.account.MFA_TICKET_HEADER_NAME
import chat.stoat.api.settings.Theme
import chat.stoat.api.settings.UserInterfaceFont
import chat.stoat.c2dm.ChannelRegistrator
import chat.stoat.persistence.KVStorage
import chat.stoat.persistence.SqlStorage
import chat.stoat.ui.theme.getDefaultFont
import chat.stoat.ui.theme.getDefaultTheme
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

fun installAppAPIHost(app: StoatApplication) {
    StoatAPIHost.install(
        config = StoatAPIConfig(
            versionName = BuildConfig.VERSION_NAME,
            applicationId = BuildConfig.APPLICATION_ID,
            isDebug = BuildConfig.DEBUG,
        ),
        storage = AppAPIStorage(app),
        platform = AppAPIPlatform(app),
    )
}

private class AppAPIStorage(private val app: StoatApplication) : StoatAPIStorage {
    override val sqlDriver: SqlDriver
        get() = SqlStorage.driver

    override val kvStorage: KVStorage by lazy { KVStorage(app) }
}

private class AppAPIPlatform(private val app: StoatApplication) : StoatAPIPlatform {
    override val defaultTheme: Theme
        get() = getDefaultTheme()

    override val defaultFont: UserInterfaceFont
        get() = getDefaultFont()

    override fun createHttpEngine(): HttpClientEngine {
        val chuckerCollector = ChuckerCollector(
            context = app,
            showNotification = true,
            retentionPeriod = RetentionManager.Period.ONE_DAY
        )

        return OkHttp.create {
            addInterceptor(
                ChuckerInterceptor.Builder(app)
                    .collector(chuckerCollector)
                    .maxContentLength(250_000L)
                    .redactHeaders(StoatAPI.TOKEN_HEADER_NAME, MFA_TICKET_HEADER_NAME)
                    .alwaysReadResponseBody(true)
                    .createShortcut(false)
                    .build()
            )
        }
    }

    override fun onRealtimeHydrated() {
        ChannelRegistrator(app).register()
    }
}
