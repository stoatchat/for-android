package chat.zekochat.settings.providers

import chat.zekochat.PeptideApplication
import chat.zekochat.persistence.KVStorage

object AgeGateUnlockedStorageProvider {
    private val kv = KVStorage(PeptideApplication.instance)

    suspend fun setAgeGateUnlocked(unlocked: Boolean) {
        kv.set("ageGateUnlocked", unlocked)
    }

    suspend fun getAgeGateUnlocked(): Boolean {
        return kv.getBoolean("ageGateUnlocked") ?: false
    }
}