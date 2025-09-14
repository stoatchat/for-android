package chat.zekochat.internals.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.XmlResourceParser
import java.util.Locale

@SuppressLint("DiscouragedApi")
fun Context.getSupportedLocales(): List<Locale> {
    val id = resources.getIdentifier(
        "_generated_res_locale_config",
        "xml",
        packageName
    )
    val localeXml = resources.getXml(id)
    val locales = ArrayList<String>()
    var event = localeXml.next()
    while (event != XmlResourceParser.END_DOCUMENT) {
        if (event == XmlResourceParser.START_TAG && localeXml.name == "locale") {
            locales.add(localeXml.getAttributeValue(0))
        }
        event = localeXml.next()
    }
    return locales.map {
        Locale.forLanguageTag(it)
    }
}