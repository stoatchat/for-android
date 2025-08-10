package chat.peptide.api.routes.microservices.autumn

import chat.peptide.api.HitRateLimitException
import chat.peptide.api.RevoltAPI
import chat.peptide.api.RevoltHttp
import chat.peptide.api.RevoltJson
import chat.peptide.api.schemas.AutumnError
import chat.peptide.api.schemas.AutumnId
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
    val uploadUrl = "${RevoltAPI.getCurrentFilesUrl()}/$tag"

    val response = RevoltHttp.post(uploadUrl) {
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
        header(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)
        onUpload { bytesSentTotal, contentLength ->
            contentLength?.let { onProgress(bytesSentTotal, it) }
        }
    }

    try {
        val autumnId = RevoltJson.decodeFromString(AutumnId.serializer(), response.bodyAsText())
        return autumnId.id
    } catch (e: Exception) {
        try {
            val error = RevoltJson.decodeFromString(AutumnError.serializer(), response.bodyAsText())
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
