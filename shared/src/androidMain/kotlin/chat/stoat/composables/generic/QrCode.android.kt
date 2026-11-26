package chat.stoat.composables.generic

import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

actual fun encodeQrCode(contents: String, size: Int): ImageBitmap {
    val matrix = QRCodeWriter().encode(
        contents,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(EncodeHintType.MARGIN to "1")
    )

    val bitmap = createBitmap(matrix.width, matrix.height)
    for (x in 0 until matrix.width) {
        for (y in 0 until matrix.height) {
            bitmap[x, y] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
        }
    }
    return bitmap.asImageBitmap()
}
