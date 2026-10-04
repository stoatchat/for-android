package chat.stoat.api.routes.safety

import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.core.model.schemas.ContentReportReason
import chat.stoat.core.model.schemas.FullMessageReport
import chat.stoat.core.model.schemas.FullServerReport
import chat.stoat.core.model.schemas.FullUserReport
import chat.stoat.core.model.schemas.MessageReport
import chat.stoat.core.model.schemas.ServerReport
import chat.stoat.core.model.schemas.UserReport
import chat.stoat.core.model.schemas.UserReportReason
import chat.stoat.api.api
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException

suspend fun putMessageReport(
    messageId: String,
    reason: ContentReportReason,
    additionalContext: String? = null
) {
    val fullMessageReport = FullMessageReport(
        content = MessageReport(
            type = "Message",
            report_reason = reason,
            id = messageId
        ),
        additional_context = additionalContext
    )

    val response = StoatHttp.post("/safety/report".api()) {
        setBody(
            StoatJson.encodeToString(
                FullMessageReport.serializer(),
                fullMessageReport
            )
        )
    }
        .bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun putServerReport(
    serverId: String,
    reason: ContentReportReason,
    additionalContext: String? = null
) {
    val fullServerReport = FullServerReport(
        content = ServerReport(
            type = "Server",
            report_reason = reason,
            id = serverId
        ),
        additional_context = additionalContext
    )

    val response = StoatHttp.post("/safety/report".api()) {
        setBody(
            StoatJson.encodeToString(
                FullServerReport.serializer(),
                fullServerReport
            )
        )
    }
        .bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun putUserReport(
    userId: String,
    reason: UserReportReason,
    additionalContext: String? = null
) {
    val fullUserReport = FullUserReport(
        content = UserReport(
            type = "User",
            report_reason = reason,
            id = userId
        ),
        additional_context = additionalContext
    )

    val response = StoatHttp.post("/safety/report".api()) {
        setBody(
            StoatJson.encodeToString(
                FullUserReport.serializer(),
                fullUserReport
            )
        )
    }
        .bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}
