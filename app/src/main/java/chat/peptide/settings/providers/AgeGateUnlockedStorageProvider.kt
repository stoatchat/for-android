package chat.peptide.settings.providers

import chat.peptide.RevoltApplication
import chat.peptide.persistence.KVStorage

object AgeGateUnlockedStorageProvider {
    private val kv = KVStorage(RevoltApplication.instance)

    suspend fun setAgeGateUnlocked(unlocked: Boolean) {
        kv.set("ageGateUnlocked", unlocked)
    }

    suspend fun getAgeGateUnlocked(): Boolean {
        return kv.getBoolean("ageGateUnlocked") ?: false
    }
}