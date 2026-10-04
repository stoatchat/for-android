package chat.stoat.api.routes.microservices.january

import chat.stoat.core.model.data.STOAT_PROXY
import io.ktor.http.encodeURLParameter

fun asJanuaryProxyUrl(url: String): String {
    return "$STOAT_PROXY/proxy?url=${url.encodeURLParameter(spaceToPlus = true)}"
}
