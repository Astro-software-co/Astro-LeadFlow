package com.astro.leadflow.shared.modules.communication.service

import com.astro.leadflow.shared.modules.communication.domain.ChatMessage
import com.astro.leadflow.shared.modules.communication.domain.ChatMessageRepository
import com.astro.leadflow.shared.modules.communication.domain.MessageDirection
import com.astro.leadflow.shared.modules.communication.domain.MessageStatus
import com.astro.leadflow.shared.modules.communication.domain.MessageType
import com.astro.leadflow.shared.modules.communication.infra.MetaClient
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.exception.ExternalServiceException
import com.astro.leadflow.shared.exception.QuotaExceededException
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import com.astro.leadflow.shared.modules.saas.service.UsageLimitService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MessageSenderService(
    private val metaClient: MetaClient,
    private val chatMessageRepository: ChatMessageRepository,
    private val usageLimitService: UsageLimitService,
    private val failureLogger: MessageDeliveryFailureLogger
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun sendText(
        lead: Lead,
        organization: Organization,
        message: String,
        marketplaceName: String? = null
    ): ChatMessage {
        checkQuota(organization, marketplaceName)
        validateWhatsappConfig(organization)

        val waMessageId = try {
            metaClient.sendTextMessage(
                phoneNumberId = organization.whatsappPhoneNumberId!!,
                accessToken = organization.whatsappToken!!,
                toPhone = lead.phoneNumber,
                message = message
            )
        } catch (e: ExternalServiceException) {
            failureLogger.logFailure(
                lead, organization,
                marketplaceName?.let { runCatching { MarketplaceType.valueOf(it) }.getOrNull() },
                "Falha ao enviar texto via Meta API", e
            )
            throw e
        }

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
        checkQuota(organization, marketplaceName)
        validateWhatsappConfig(organization)

        val waMessageId = try {
            metaClient.sendTemplateMessage(
                phoneNumberId = organization.whatsappPhoneNumberId!!,
                accessToken = organization.whatsappToken!!,
                toPhone = lead.phoneNumber,
                templateName = templateName
            )
        } catch (e: ExternalServiceException) {
            failureLogger.logFailure(
                lead, organization,
                marketplaceName?.let { runCatching { MarketplaceType.valueOf(it) }.getOrNull() },
                "Falha ao enviar template '$templateName' via Meta API", e
            )
            throw e
        }

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

    private fun checkQuota(organization: Organization, marketplaceName: String?) {
        if (!usageLimitService.canSendMessage(organization.id, marketplaceName)) {
            throw QuotaExceededException(
                "Limite de mensagens atingido para ${marketplaceName ?: "organização ${organization.id}"}."
            )
        }
    }

    private fun validateWhatsappConfig(organization: Organization) {
        requireNotNull(organization.whatsappPhoneNumberId) {
            "PhoneNumberId não configurado para organização ${organization.id}"
        }
        requireNotNull(organization.whatsappToken) {
            "WhatsApp token não configurado para organização ${organization.id}"
        }
    }
}