package chat.stoat.api.internals

import kotlin.experimental.and
import kotlin.random.Random

fun String.isUlid(): Boolean {
    return "[0-9A-HJKMNP-TV-Z]{26}".toRegex().matches(this)
}

object ULID {
    private const val entropy = 10
    private const val len = 26

    private const val maxTimestamp = 281474976710655L
    private const val minTimestamp = 0L

    private val b32chars = charArrayOf(
        0x30.toChar(), 0x31.toChar(), 0x32.toChar(), 0x33.toChar(), 0x34.toChar(), 0x35.toChar(),
        0x36.toChar(), 0x37.toChar(), 0x38.toChar(), 0x39.toChar(), 0x41.toChar(), 0x42.toChar(),
        0x43.toChar(), 0x44.toChar(), 0x45.toChar(), 0x46.toChar(), 0x47.toChar(), 0x48.toChar(),
        0x4a.toChar(), 0x4b.toChar(), 0x4d.toChar(), 0x4e.toChar(), 0x50.toChar(), 0x51.toChar(),
        0x52.toChar(), 0x53.toChar(), 0x54.toChar(), 0x56.toChar(), 0x57.toChar(), 0x58.toChar(),
        0x59.toChar(), 0x5a.toChar()
    )

    fun makeSpecial(timestamp: Long, entropy: ByteArray = fetchEntropy()): String {
        if (timestamp !in minTimestamp..maxTimestamp) {
            throw IllegalArgumentException("timestamp out of range: $timestamp")
        }

        if (entropy.size != this.entropy) {
            throw IllegalArgumentException("entropy must be exactly ${this.entropy} bytes")
        }

        val chars = CharArray(len)

        // Time part (10 chars)
        chars[0] = b32chars[timestamp.ushr(45).toInt() and 0x1f]
        chars[1] = b32chars[timestamp.ushr(40).toInt() and 0x1f]
        chars[2] = b32chars[timestamp.ushr(35).toInt() and 0x1f]
        chars[3] = b32chars[timestamp.ushr(30).toInt() and 0x1f]
        chars[4] = b32chars[timestamp.ushr(25).toInt() and 0x1f]
        chars[5] = b32chars[timestamp.ushr(20).toInt() and 0x1f]
        chars[6] = b32chars[timestamp.ushr(15).toInt() and 0x1f]
        chars[7] = b32chars[timestamp.ushr(10).toInt() and 0x1f]
        chars[8] = b32chars[timestamp.ushr(5).toInt() and 0x1f]
        chars[9] = b32chars[timestamp.toInt() and 0x1f]

        // Entropy part (16 chars)
        chars[10] = b32chars[(entropy[0].toShort() and 0xff).toInt().ushr(3)]
        chars[11] =
            b32chars[
                (
                        entropy[0].toInt() shl 2 or (entropy[1].toShort() and 0xff).toInt()
                            .ushr(6) and 0x1f
                        )
            ]
        chars[12] = b32chars[((entropy[1].toShort() and 0xff).toInt().ushr(1) and 0x1f)]
        chars[13] =
            b32chars[
                (
                        entropy[1].toInt() shl 4 or (entropy[2].toShort() and 0xff).toInt()
                            .ushr(4) and 0x1f
                        )
            ]
        chars[14] =
            b32chars[
                (
                        entropy[2].toInt() shl 5 or (entropy[3].toShort() and 0xff).toInt()
                            .ushr(7) and 0x1f
                        )
            ]
        chars[15] = b32chars[((entropy[3].toShort() and 0xff).toInt().ushr(2) and 0x1f)]
        chars[16] =
            b32chars[
                (
                        entropy[3].toInt() shl 3 or (entropy[4].toShort() and 0xff).toInt()
                            .ushr(5) and 0x1f
                        )
            ]
        chars[17] = b32chars[(entropy[4].toInt() and 0x1f)]
        chars[18] = b32chars[(entropy[5].toShort() and 0xff).toInt().ushr(3)]
        chars[19] =
            b32chars[
                (
                        entropy[5].toInt() shl 2 or (entropy[6].toShort() and 0xff).toInt()
                            .ushr(6) and 0x1f
                        )
            ]
        chars[20] = b32chars[((entropy[6].toShort() and 0xff).toInt().ushr(1) and 0x1f)]
        chars[21] =
            b32chars[
                (
                        entropy[6].toInt() shl 4 or (entropy[7].toShort() and 0xff).toInt()
                            .ushr(4) and 0x1f
                        )
            ]
        chars[22] =
            b32chars[
                (
                        entropy[7].toInt() shl 5 or (entropy[8].toShort() and 0xff).toInt()
                            .ushr(7) and 0x1f
                        )
            ]
        chars[23] = b32chars[((entropy[8].toShort() and 0xff).toInt().ushr(2) and 0x1f)]
        chars[24] =
            b32chars[
                (
                        entropy[8].toInt() shl 3 or (entropy[9].toShort() and 0xff).toInt()
                            .ushr(5) and 0x1f
                        )
            ]
        chars[25] = b32chars[(entropy[9].toInt() and 0x1f)]

        return String(chars)
    }

    private fun fetchEntropy(): ByteArray {
        val bytes = ByteArray(entropy)
        Random.nextBytes(bytes)
        return bytes
    }

    fun makeNext(): String {
        return makeSpecial(System.currentTimeMillis(), fetchEntropy())
    }

    fun asTimestamp(ulid: String): Long {
        if (ulid.length != len) {
            throw IllegalArgumentException("ULID must be exactly $len characters")
        }

        var timestamp = 0L

        for (i in 0..9) {
            val char = ulid[i]
            val value = b32chars.indexOf(char)

            if (value == -1) {
                throw IllegalArgumentException("Invalid character '$char' at position $i")
            }

            timestamp = timestamp or (value.toLong() shl (9 - i) * 5)
        }

        return timestamp
    }
}
