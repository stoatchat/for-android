package chat.zekochat.api.routes.microservices.autumn

import chat.zekochat.api.HitRateLimitException
import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import chat.zekochat.api.schemas.AutumnError
import chat.zekochat.api.schemas.AutumnId
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.io.File

const val MAX_ATTACHMENTS_PER_MESSAGE = 5

data class FileArgs(
    val file: File,
    val filename: String,
    val contentType: String,
    val spoiler: Boolean = false,
    val pickerIdentifier: String? = null,
)

suspend fun uploadToAutumn(
    file: File,
    name: String,
    tag: String,
    contentType: ContentType,
    onProgress: (Long, Long) -> Unit = { _, _ -> }
): String {
    val uploadUrl = "${PeptideAPI.getCurrentFilesUrl()}/$tag"

    val response = PeptideHttp.post(uploadUrl) {
        setBody(
            MultiPartFormDataContent(
                formData {
                    append(
                        "file",
                        file.readBytes(),
                        Headers.build {
                            append(HttpHeaders.ContentType, contentType.toString())
                            append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
                        }
                    )
                }
            )
        )
        header(PeptideAPI.TOKEN_HEADER_NAME, PeptideAPI.sessionToken)
        onUpload { bytesSentTotal, contentLength ->
            contentLength?.let { onProgress(bytesSentTotal, it) }
        }
    }

    try {
        val autumnId = PeptideJson.decodeFromString(AutumnId.serializer(), response.bodyAsText())
        return autumnId.id
    } catch (e: Exception) {
        try {
            val error = PeptideJson.decodeFromString(AutumnError.serializer(), response.bodyAsText())
            throw Exception(error.type)
        } catch (e: Exception) {
            if (response.status == HttpStatusCode.TooManyRequests) {
                throw HitRateLimitException()
            }
            if (response.status == HttpStatusCode.PayloadTooLarge) {
                throw Exception("File too large")
            }
            throw Exception("Unknown error")
        }
    }
}
