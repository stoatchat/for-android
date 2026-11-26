package chat.stoat.screens.changelogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal actual fun rememberFormattedDate(isoDateTime: String?): String? {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val zone = remember { ZoneId.systemDefault() }
    return remember(isoDateTime, formatter, zone) {
        isoDateTime?.let {
            Instant.parse(it).atZone(zone).toLocalDate().format(formatter)
        }
    }
}
