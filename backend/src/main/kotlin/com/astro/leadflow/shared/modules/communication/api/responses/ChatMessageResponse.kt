package com.astro.leadflow.shared.modules.communication.api.responses

import com.astro.leadflow.shared.modules.communication.domain.MessageDirection
import com.astro.leadflow.shared.modules.communication.domain.MessageStatus
import com.astro.leadflow.shared.modules.communication.domain.MessageType
import java.time.LocalDateTime
import java.util.UUID

data class ChatMessageResponse(
    val id: UUID,
    val leadId: UUID,
    val direction: MessageDirection,
    val content: String,
    val messageType: MessageType,
    val status: MessageStatus,
    val sentAt: LocalDateTime
)