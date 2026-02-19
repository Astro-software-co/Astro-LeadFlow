package com.astro.leadflow.modules.communication.service

import com.astro.leadflow.modules.communication.domain.*
import com.astro.leadflow.modules.communication.infra.MetaClient
import com.astro.leadflow.modules.crm.domain.Lead
import com.astro.leadflow.modules.iam.domain.Organization
import com.astro.leadflow.modules.saas.service.UsageLimitService
import com.astro.leadflow.shared.exception.QuotaExceededException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MessageSenderService(
    private val metaClient: MetaClient,
    private val chatMessageRepository: ChatMessageRepository,
    private val usageLimitService: UsageLimitService
) {

    @Transactional
    fun sendText(
        lead: Lead,
        organization: Organization,
        message: String,
        marketplaceName: String? = null
    ): ChatMessage {
        // Verifica quota antes de enviar
        if (!usageLimitService.canSendMessage(organization.id, marketplaceName)) {
            throw QuotaExceededException(
                "Limite de mensagens atingido para ${marketplaceName ?: "esta organização"}."
            )
        }

        val waMessageId = metaClient.sendTextMessage(
            phoneNumberId = organization.whatsappPhoneNumberId
                ?: error("PhoneNumberId não configurado para organização ${organization.id}"),
            accessToken = organization.whatsappToken
                ?: error("WhatsApp token não configurado para organização ${organization.id}"),
            toPhone = lead.phoneNumber,
            message = message
        )

        // Registra mensagem no histórico
        val chatMessage = chatMessageRepository.save(
            ChatMessage(
                lead = lead,
                organization = organization,
                direction = MessageDirection.OUTBOUND,
                content = message,
                messageType = MessageType.TEXT,
                waMessageId = waMessageId,
                status = MessageStatus.SENT
            )
        )

        // Incrementa contador de uso
        usageLimitService.incrementUsage(organization.id, marketplaceName)

        return chatMessage
    }

    @Transactional
    fun sendTemplate(
        lead: Lead,
        organization: Organization,
        templateName: String,
        marketplaceName: String? = null
    ): ChatMessage {
        if (!usageLimitService.canSendMessage(organization.id, marketplaceName)) {
            throw QuotaExceededException()
        }

        val waMessageId = metaClient.sendTemplateMessage(
            phoneNumberId = organization.whatsappPhoneNumberId!!,
            accessToken = organization.whatsappToken!!,
            toPhone = lead.phoneNumber,
            templateName = templateName
        )

        val chatMessage = chatMessageRepository.save(
            ChatMessage(
                lead = lead,
                organization = organization,
                direction = MessageDirection.OUTBOUND,
                content = "[Template: $templateName]",
                messageType = MessageType.TEMPLATE,
                waMessageId = waMessageId,
                status = MessageStatus.SENT
            )
        )

        usageLimitService.incrementUsage(organization.id, marketplaceName)

        return chatMessage
    }
}