package chat.peptide.api.routes.microservices.january

import chat.peptide.api.RevoltAPI
import java.net.URLEncoder

fun asJanuaryProxyUrl(url: String): String {
    return "${RevoltAPI.getCurrentJanuaryUrl()}/proxy?url=${URLEncoder.encode(url, "utf-8")}"
}
