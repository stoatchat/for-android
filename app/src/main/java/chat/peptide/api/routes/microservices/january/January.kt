package chat.peptide.api.routes.microservices.january

import chat.peptide.api.PeptideAPI
import java.net.URLEncoder

fun asJanuaryProxyUrl(url: String): String {
    return "${PeptideAPI.getCurrentJanuaryUrl()}/proxy?url=${URLEncoder.encode(url, "utf-8")}"
}
