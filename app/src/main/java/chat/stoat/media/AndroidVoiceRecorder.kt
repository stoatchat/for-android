package chat.stoat.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.muxer.BufferInfo
import androidx.media3.muxer.OggMuxer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.time.Duration.Companion.milliseconds

private data class AndroidVoiceRecorderFormatSpec(
    val outputFormat: Int,
    val audioEncoder: Int,
    val mimeType: String,
    val fileExtension: String,
    val samplingRate: Int,
    val bitRate: Int,
    val requiresOggRemux: Boolean
)

class AndroidVoiceRecorder(private val context: Context) : VoiceRecorder {
    private val samplingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val amplitudeSamples = mutableListOf<Int>()
    private val amplitudeSamplesLock = Any()

    private var recorder: MediaRecorder? = null
    private var amplitudeSamplingJob: Job? = null
    private var outputFile: File? = null
    private var format: AndroidVoiceRecorderFormatSpec? = null
    private var startedAtMillis: Long = 0L

    override fun start() {
        check(recorder == null) { "Recording already running" }

        val format = chooseFormat()

        val file = File.createTempFile(
            "vcr_",
            ".${format.fileExtension}",
            context.cacheDir,
        )

        val mediaRecorder = createMediaRecorder()

        try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)

                setOutputFormat(format.outputFormat)
                setAudioEncoder(format.audioEncoder)

                setAudioChannels(1)

                setAudioSamplingRate(format.samplingRate)
                setAudioEncodingBitRate(format.bitRate)

                setOutputFile(file.absolutePath)

                setMaxDuration(VoiceRecorder.MAX_DURATION_MILLIS)

                prepare()
                start()
            }

            recorder = mediaRecorder
            outputFile = file
            this.format = format
            startedAtMillis = SystemClock.elapsedRealtime()
            synchronized(amplitudeSamplesLock) {
                amplitudeSamples.clear()
            }
            amplitudeSamplingJob = samplingScope.launch {
                while (isActive) {
                    delay(AMPLITUDE_SAMPLE_INTERVAL_MILLIS.milliseconds)
                    val amplitude = try {
                        mediaRecorder.maxAmplitude
                    } catch (_: IllegalStateException) {
                        break
                    }
                    synchronized(amplitudeSamplesLock) {
                        amplitudeSamples.add(amplitude)
                    }
                }
            }
        } catch (e: Exception) {
            mediaRecorder.release()
            file.delete()
            throw e
        }
    }

    override suspend fun finish(): Recording? {
        amplitudeSamplingJob?.cancelAndJoin()
        amplitudeSamplingJob = null
        return withContext(Dispatchers.IO) {
            finishImpl()
        }
    }

    private fun finishImpl(): Recording? {
        val recorder = recorder ?: return null
        val file = outputFile ?: return null
        val format = format ?: return null

        val elapsedDuration = SystemClock.elapsedRealtime() - startedAtMillis

        var remuxedFile: File? = null

        try {
            try {
                recorder.stop()
            } catch (_: RuntimeException) {
                return null
            } finally {
                recorder.release()
                this.recorder = null
            }

            val finalizedFile = if (format.requiresOggRemux) {
                val targetFile = File.createTempFile(
                    "vcr_remuxed_",
                    ".${format.fileExtension}",
                    context.cacheDir
                )
                remuxedFile = targetFile

                try {
                    remuxOpusOgg(file, targetFile)
                    targetFile
                } catch (_: Exception) {
                    return null
                }
            } else {
                file
            }

            val durationMillis = readDurationMillis(finalizedFile) ?: elapsedDuration
            val waveform = synchronized(amplitudeSamplesLock) {
                buildWaveform(amplitudeSamples.toList())
            }

            return Recording(
                bytes = finalizedFile.readBytes(),
                mimeType = format.mimeType,
                fileExtension = format.fileExtension,
                durationMillis = durationMillis,
                waveform = waveform,
            )
        } finally {
            file.delete()
            remuxedFile?.delete()
            outputFile = null
            this.format = null
            startedAtMillis = 0L
            synchronized(amplitudeSamplesLock) {
                amplitudeSamples.clear()
            }
        }
    }

    private fun chooseFormat(): AndroidVoiceRecorderFormatSpec {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AndroidVoiceRecorderFormatSpec(
                outputFormat = MediaRecorder.OutputFormat.OGG,
                audioEncoder = MediaRecorder.AudioEncoder.OPUS,
                mimeType = "audio/ogg",
                fileExtension = "ogg",
                samplingRate = 48_000,
                bitRate = 32_000,
                requiresOggRemux = true
            )
        } else {
            AndroidVoiceRecorderFormatSpec(
                outputFormat = MediaRecorder.OutputFormat.MPEG_4,
                audioEncoder = MediaRecorder.AudioEncoder.AAC,
                mimeType = "audio/mp4",
                fileExtension = "m4a",
                samplingRate = 44_100,
                bitRate = 48_000,
                requiresOggRemux = false
            )
        }
    }

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private fun readDurationMillis(file: File): Long? {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(file.absolutePath)
            retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun remuxOpusOgg(inputFile: File, outputFile: File) {
        // Rebuild the platform recorder's Ogg pages so their granule positions and
        // final EOS page are valid for strict demuxers such as Firefox's.
        val extractor = MediaExtractor()

        try {
            extractor.setDataSource(inputFile.absolutePath)

            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME) ==
                        MediaFormat.MIMETYPE_AUDIO_OPUS
            } ?: error("No Opus audio track found")
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val opusHead = inputFormat.getByteBuffer("csd-0")
                ?.copyBytes()
                ?: error("Opus identification header missing")

            val outputFormat = Format.Builder()
                .setSampleMimeType(MimeTypes.AUDIO_OPUS)
                .setChannelCount(inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
                .setSampleRate(inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE))
                .setInitializationData(listOf(opusHead))
                .build()

            extractor.selectTrack(trackIndex)

            FileOutputStream(outputFile).channel.use { outputChannel ->
                OggMuxer.Builder(outputChannel).build().use { muxer ->
                    val outputTrack = muxer.addTrack(outputFormat)
                    val sampleBuffer = ByteBuffer.allocate(MAX_OPUS_PACKET_SIZE_BYTES)

                    while (true) {
                        sampleBuffer.clear()
                        val sampleSize = extractor.readSampleData(sampleBuffer, 0)
                        if (sampleSize < 0) {
                            break
                        }

                        sampleBuffer.position(0)
                        sampleBuffer.limit(sampleSize)
                        muxer.writeSampleData(
                            outputTrack,
                            sampleBuffer,
                            BufferInfo(
                                extractor.sampleTime.coerceAtLeast(0),
                                sampleSize,
                                0
                            )
                        )
                        extractor.advance()
                    }
                }
            }
        } finally {
            extractor.release()
        }
    }

    private fun ByteBuffer.copyBytes(): ByteArray {
        val copy = duplicate().apply { position(0) }
        return ByteArray(copy.remaining()).also { copy.get(it) }
    }

    private companion object {
        const val AMPLITUDE_SAMPLE_INTERVAL_MILLIS = 50L
        const val MAX_OPUS_PACKET_SIZE_BYTES = 64 * 1024
    }
}
