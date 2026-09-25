package chat.stoat.media

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class Recording(
    val bytes: ByteArray,
    val mimeType: String,
    val fileExtension: String,
    val durationMillis: Long,
    val waveform: List<Int>,
    val isSilent: Boolean,
) {
    fun toMessageContent(): String = buildJsonObject {
        put("x", "chat.stoat.VoiceMessage")
        put("y", 1)
        put("z", durationMillis)
        put("w", buildJsonArray {
            waveform.forEach { add(it) }
        })
    }.toString()
}

internal data class VoiceMessageMetadata(
    val durationMillis: Long,
    val waveform: List<Int>,
)

internal fun parseVoiceMessageMetadata(content: String?): VoiceMessageMetadata? {
    if (content == null) return null

    val metadata = runCatching {
        Json.parseToJsonElement(content) as? JsonObject
    }.getOrNull() ?: return null
    if (metadata.keys != setOf("x", "y", "z", "w")) return null

    val id = metadata["x"] as? JsonPrimitive ?: return null
    if (!id.isString || id.content != "chat.stoat.VoiceMessage") return null

    val version = metadata["y"] as? JsonPrimitive ?: return null
    if (version.isString || version.intOrNull != 1) return null

    val duration = metadata["z"] as? JsonPrimitive ?: return null
    val durationMillis = duration.longOrNull
    if (
        duration.isString ||
        durationMillis == null ||
        durationMillis !in VoiceRecorder.MIN_DURATION_MILLIS..
        VoiceRecorder.MAX_DURATION_MILLIS.toLong()
    ) {
        return null
    }

    val waveform = metadata["w"] as? JsonArray ?: return null
    if (waveform.isEmpty() || waveform.size > VoiceRecorder.WAVEFORM_SAMPLE_COUNT) return null

    val samples = waveform.map { sample ->
        val value = sample as? JsonPrimitive ?: return null
        if (value.isString) return null
        value.intOrNull?.takeIf { it in 0..UByte.MAX_VALUE.toInt() } ?: return null
    }
    return VoiceMessageMetadata(durationMillis, samples)
}

interface VoiceRecorder {
    fun start()
    suspend fun finish(): Recording?

    companion object {
        const val MIN_DURATION_MILLIS = 1_000L
        const val POST_ROLL_DURATION_MILLIS = 300L
        const val MAX_DURATION_MILLIS = 60_000 * 15 // 15 minutes
        const val WAVEFORM_SAMPLE_COUNT = 64
        const val SILENCE_AMPLITUDE_THRESHOLD = 1_000
    }
}

/**
 * Reduce recorder amplitude readings to a normalised waveform.
 */
internal fun buildWaveform(
    amplitudes: List<Int>,
    sampleCount: Int = VoiceRecorder.WAVEFORM_SAMPLE_COUNT,
): List<Int> {
    require(sampleCount > 0) { "sampleCount must be positive" }

    if (amplitudes.isEmpty()) return List(sampleCount) { 0 }

    val peaks = List(sampleCount) { index ->
        val start = index * amplitudes.size / sampleCount
        val end = ceil((index + 1) * amplitudes.size.toDouble() / sampleCount)
            .toInt()
            .coerceAtLeast(start + 1)
            .coerceAtMost(amplitudes.size)
        amplitudes.subList(start.coerceAtMost(amplitudes.lastIndex), end)
            .maxOrNull()
            ?.coerceAtLeast(0)
            ?: 0
    }
    val maximum = peaks.maxOrNull()?.takeIf { it > 0 } ?: return List(sampleCount) { 0 }

    return peaks.map { amplitude ->
        // Square-root scaling, to avoid a peak flattening the whole waveform
        (sqrt(amplitude.toDouble() / maximum) * UByte.MAX_VALUE.toInt())
            .roundToInt()
            .coerceIn(0, UByte.MAX_VALUE.toInt())
    }
}
