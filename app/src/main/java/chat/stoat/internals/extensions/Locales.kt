package chat.stoat.internals.extensions

import android.content.Context
import android.content.res.XmlResourceParser
import chat.stoat.R
import java.util.Locale

fun Context.getSupportedLocales(): List<Locale> {
    val localeXml = resources.getXml(R.xml.locales_config)
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