package chat.stoat.api.routes.microservices.autumn

import chat.stoat.api.HitRateLimitException
import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.AutumnError
import chat.stoat.core.model.schemas.AutumnId
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
    val uploadUrl = "$STOAT_FILES/$tag"

    val response = StoatHttp.post(uploadUrl) {
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
        header(StoatAPI.TOKEN_HEADER_NAME, StoatAPI.sessionToken)
        onUpload { bytesSentTotal, contentLength ->
            contentLength?.let { onProgress(bytesSentTotal, it) }
        }
    }

    try {
        val autumnId = StoatJson.decodeFromString(AutumnId.serializer(), response.bodyAsText())
        return autumnId.id
    } catch (e: Exception) {
        try {
            val error = StoatJson.decodeFromString(AutumnError.serializer(), response.bodyAsText())
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
