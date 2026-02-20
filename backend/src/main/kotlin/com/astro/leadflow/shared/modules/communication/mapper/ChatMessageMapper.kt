package com.astro.leadflow.modules.communication.mapper

import com.astro.leadflow.modules.communication.api.responses.ChatMessageResponse
import com.astro.leadflow.modules.communication.domain.ChatMessage
import org.springframework.stereotype.Component

@Component
class ChatMessageMapper {

    fun toResponse(message: ChatMessage): ChatMessageResponse = ChatMessageResponse(
            id = message.id,
            leadId = message.lead.id,
            direction = message.direction,
            content = message.content,
            messageType = message.messageType,
            status = message.status,
            sentAt = message.sentAt
    )
}