package chat.stoat.composables.generic

import chat.stoat.api.routes.microservices.autumn.uploadToAutumn
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.mimeType
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.ktor.http.ContentType
import kotlin.coroutines.cancellation.CancellationException
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** A URL that [chat.stoat.core.designsystem.components.RemoteImage] can load to preview [this]. */
expect fun PlatformFile.previewUrl(): String

/** The image to show for a media model: either a picked [PlatformFile] or a remote URL. */
fun mediaModelUrl(model: Any?): String? = when (model) {
    is PlatformFile -> model.previewUrl()
    is String -> model
    else -> null
}

/**
 * Uploads a picked file to Autumn under [tag], using its own name and MIME type.
 *
 * @param readError shown instead of the underlying error if the file can't be read
 */
suspend fun uploadPickedFile(
    file: PlatformFile,
    tag: String,
    fallbackName: String,
    readError: StringResource? = null,
    onProgress: (Long, Long) -> Unit = { _, _ -> },
): String {
    val bytes = try {
        file.readBytes()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (readError == null) throw e
        throw IllegalArgumentException(getString(readError), e)
    }
    return uploadToAutumn(
        bytes = bytes,
        name = file.name.ifBlank { fallbackName },
        tag = tag,
        contentType = ContentType.parse(file.mimeType()?.toString() ?: "image/*"),
        onProgress = onProgress,
    )
}
