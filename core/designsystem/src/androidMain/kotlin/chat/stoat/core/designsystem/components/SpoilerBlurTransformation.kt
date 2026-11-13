package chat.stoat.core.designsystem.components

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

internal object SpoilerBlurTransformation : BitmapTransformation() {
    private const val ID = "chat.stoat.core.designsystem.components.SpoilerBlurTransformation.1"

    // found through trial & error
    private const val SIZE = 48
    private const val RADIUS = 1
    private const val PASSES = 2

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val ratio = SIZE.toFloat() / max(toTransform.width, toTransform.height)
        val width = (toTransform.width * ratio).roundToInt().coerceAtLeast(1)
        val height = (toTransform.height * ratio).roundToInt().coerceAtLeast(1)
        val small = toTransform.scale(width, height).copy(Bitmap.Config.ARGB_8888, true)

        val pixels = IntArray(width * height)
        small.getPixels(pixels, 0, width, 0, 0, width, height)
        val scratch = IntArray(pixels.size)
        repeat(PASSES) {
            boxBlur(pixels, scratch, width, height, horizontal = true)
            boxBlur(scratch, pixels, width, height, horizontal = false)
        }
        small.setPixels(pixels, 0, width, 0, 0, width, height)
        return small
    }

    private fun boxBlur(
        src: IntArray,
        dst: IntArray,
        width: Int,
        height: Int,
        horizontal: Boolean
    ) {
        val lines = if (horizontal) height else width
        val length = if (horizontal) width else height
        for (line in 0 until lines) {
            for (i in 0 until length) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0
                for (k in -RADIUS..RADIUS) {
                    val j = (i + k).coerceIn(0, length - 1)
                    val p = if (horizontal) src[line * width + j] else src[j * width + line]
                    a += p ushr 24
                    r += (p shr 16) and 0xFF
                    g += (p shr 8) and 0xFF
                    b += p and 0xFF
                }
                val n = RADIUS * 2 + 1
                val index = if (horizontal) line * width + i else i * width + line
                dst[index] = ((a / n) shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
            }
        }
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID.toByteArray())
    }

    override fun equals(other: Any?): Boolean = other is SpoilerBlurTransformation

    override fun hashCode(): Int = ID.hashCode()
}
