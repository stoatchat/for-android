package chat.stoat.composables.generic

import androidx.compose.ui.graphics.ImageBitmap

/** Encodes [contents] as a black-on-white QR code of about [size] pixels. Blocks while encoding. */
expect fun encodeQrCode(contents: String, size: Int): ImageBitmap
