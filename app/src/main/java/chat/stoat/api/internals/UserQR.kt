package chat.stoat.api.internals

import androidx.core.net.toUri
import chat.stoat.api.StoatCbor
import chat.stoat.core.model.data.STOAT_MARKETING
import chat.stoat.core.model.schemas.User
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class UserQRContents(
    val format: String,
    val avatar: String,
    val displayName: String,
    val username: String,
    val discriminator: String,
    val id: String,
)

object UserQR {
    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    fun contents(user: User): String {
        return "$STOAT_MARKETING/qr?" + Base64.encode(
            StoatCbor.encodeToByteArray(
                UserQRContents.serializer(),
                UserQRContents(
                    format = $$"rqr$user$0",
                    avatar = user.avatar?.id
                        ?: "01JDZRBY95P8AY4CFVX16FFVWS", // Sentinel value for missing avatar
                    displayName = user.displayName
                        ?: "01JDZRDD1YZ84HA8EST2E5GVXT", // Sentinel value for missing display name
                    username = user.username
                        ?: "01JDZRBAG7AN9PGKKC5GSR7Z72", // Sentinel value for missing username
                    discriminator = user.discriminator ?: "0000",
                    id = user.id ?: "01JDZRDSJXH77K36XAFG1GX9JY" // Sentinel value for missing id
                )
            )
        )
    }

    @OptIn(ExperimentalEncodingApi::class, ExperimentalSerializationApi::class)
    fun fromUri(uriString: String): UserQRContents? {
        return try {
            val uri = uriString.toUri()
            val base64 = uri.query ?: return null
            val decodedBytes = Base64.decode(base64)
            StoatCbor.decodeFromByteArray(UserQRContents.serializer(), decodedBytes)
        } catch (e: Exception) {
            null
        }
    }
}