package chat.stoat.api.routes.microservices.autumn

import io.ktor.http.ContentType
import java.io.File

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
): String = uploadToAutumn(file.readBytes(), name, tag, contentType, onProgress)
