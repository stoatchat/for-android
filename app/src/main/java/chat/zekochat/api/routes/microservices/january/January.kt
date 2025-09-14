package chat.zekochat.api.routes.microservices.january

import chat.zekochat.api.PeptideAPI
import java.net.URLEncoder

fun asJanuaryProxyUrl(url: String): String {
    return "${PeptideAPI.getCurrentJanuaryUrl()}/proxy?url=${URLEncoder.encode(url, "utf-8")}"
}
