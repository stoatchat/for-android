package chat.peptide.api.settings

enum class NotificationType(val storageValue: String) {
    DEFAULT("default"),
    MUTED("muted"),
    ALL("all"),
    MENTIONS_ONLY("mentions"),
    NONE("none");

    companion object {
        fun fromStorage(value: String?): NotificationType {
            return when (value) {
                null -> DEFAULT
                "default" -> DEFAULT
                "muted" -> MUTED
                "all" -> ALL
                "mentions" -> MENTIONS_ONLY
                "none" -> NONE
                else -> DEFAULT
            }
        }
    }
}


