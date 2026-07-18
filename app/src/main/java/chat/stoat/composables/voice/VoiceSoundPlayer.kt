package chat.stoat.composables.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import androidx.annotation.RawRes
import chat.stoat.R
import logcat.LogPriority
import logcat.logcat

internal enum class VoiceSound(@param:RawRes val resourceId: Int) {
    MUTE(R.raw.sfx_mute),
    UNMUTE(R.raw.sfx_unmute),
    DEAFEN(R.raw.sfx_deafen),
    UNDEAFEN(R.raw.sfx_undeafen),
    USER_JOIN(R.raw.sfx_user_join_voice),
    USER_LEAVE(R.raw.sfx_user_leave_voice),
    STREAM_START(R.raw.sfx_stream_start),
    STREAM_END(R.raw.sfx_stream_end),
}

internal class VoiceSoundPlayer(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = mutableMapOf<VoiceSound, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()
    private val pendingSounds = mutableListOf<VoiceSound>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) {
                logcat(LogPriority.ERROR) {
                    "Failed to load voice sound sample $sampleId (status $status)"
                }
                return@setOnLoadCompleteListener
            }

            loadedSoundIds += sampleId
            pendingSounds
                .filter { soundIds[it] == sampleId }
                .forEach(::playLoaded)
            pendingSounds.removeAll { soundIds[it] == sampleId }
        }
        VoiceSound.entries.forEach { sound ->
            soundIds[sound] =
                soundPool.load(context.applicationContext, sound.resourceId, 1)
        }
    }

    fun play(sound: VoiceSound) {
        val soundId = soundIds.getValue(sound)
        if (soundId in loadedSoundIds) {
            playLoaded(sound)
        } else {
            pendingSounds += sound
        }
    }

    fun release() {
        pendingSounds.clear()
        // let a leave sound finish
        Handler(Looper.getMainLooper()).postDelayed(soundPool::release, 1_000)
    }

    private fun playLoaded(sound: VoiceSound) {
        val streamId = soundPool.play(soundIds.getValue(sound), 1f, 1f, 1, 0, 1f)
        logcat { "Playing voice sound $sound (stream $streamId)" }
        if (streamId == 0) {
            logcat(LogPriority.ERROR) { "SoundPool could not play $sound" }
        }
    }
}
