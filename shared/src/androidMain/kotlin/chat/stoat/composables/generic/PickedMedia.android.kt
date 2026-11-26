package chat.stoat.composables.generic

import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile

actual fun PlatformFile.previewUrl(): String = when (val file = androidFile) {
    is AndroidFile.UriWrapper -> file.uri.toString()
    is AndroidFile.FileWrapper -> file.file.toURI().toString()
}
