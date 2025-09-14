package chat.zekochat.api.routes.safety

import chat.zekochat.api.PeptideError
import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import chat.zekochat.api.schemas.ContentReportReason
import chat.zekochat.api.schemas.FullMessageReport
import chat.zekochat.api.schemas.FullServerReport
import chat.zekochat.api.schemas.FullUserReport
import chat.zekochat.api.schemas.MessageReport
import chat.zekochat.api.schemas.ServerReport
import chat.zekochat.api.schemas.UserReport
import chat.zekochat.api.schemas.UserReportReason
import chat.zekochat.api.api
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

    val response = PeptideHttp.post("/safety/report".api()) {
        setBody(
            PeptideJson.encodeToString(
                FullMessageReport.serializer(),
                fullMessageReport
            )
        )
    }
        .bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
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

    val response = PeptideHttp.post("/safety/report".api()) {
        setBody(
            PeptideJson.encodeToString(
                FullServerReport.serializer(),
                fullServerReport
            )
        )
    }
        .bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
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

    val response = PeptideHttp.post("/safety/report".api()) {
        setBody(
            PeptideJson.encodeToString(
                FullUserReport.serializer(),
                fullUserReport
            )
        )
    }
        .bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}
