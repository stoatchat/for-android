package chat.stoat.core.model.data

// Mutable: overridden at runtime by InstanceManager when the user picks a custom
// self-hosted server from the login screen. These values are the default instance
// (official Stoat) used until a custom instance is set.
var STOAT_BASE = "https://api.stoat.chat/0.8"
var STOAT_FILES = "https://cdn.stoatusercontent.com"
var STOAT_PROXY = "https://proxy.stoatusercontent.com"
var STOAT_WEB_APP = "https://stoat.chat"
var STOAT_WEBSOCKET = "wss://events.stoat.chat"

const val STOAT_SUPPORT = "https://support.stoat.chat"
const val STOAT_MARKETING = "https://stoat.chat"
const val STOAT_BETA_WEB_APP = "https://beta.stoat.chat"
const val STOAT_INVITES = "https://stt.gg"
const val STOAT_CHANGELOG = "https://changelog.stoat.chat"
