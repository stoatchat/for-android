package chat.stoat.api.routes.microservices.january

import chat.stoat.core.model.data.STOAT_PROXY
import java.net.URLEncoder

fun asJanuaryProxyUrl(url: String): String {
    return "$STOAT_PROXY/proxy?url=${URLEncoder.encode(url, "utf-8")}"
}
