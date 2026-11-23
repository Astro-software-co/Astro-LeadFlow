package com.astro.leadflow.shared.modules.communication.service

import com.astro.leadflow.shared.modules.communication.domain.ChatMessage
import com.astro.leadflow.shared.modules.communication.domain.ChatMessageRepository
import com.astro.leadflow.shared.modules.communication.domain.MessageDirection
import com.astro.leadflow.shared.modules.communication.domain.MessageStatus
import com.astro.leadflow.shared.modules.communication.domain.MessageType
import com.astro.leadflow.shared.modules.communication.domain.ChatMessageSenderType
import com.astro.leadflow.shared.modules.communication.meta.MetaApiClient
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.exception.ExternalServiceException
import com.astro.leadflow.shared.exception.QuotaExceededException
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import com.astro.leadflow.shared.modules.saas.service.UsageLimitService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MessageSenderService(
    private val metaApiClient: MetaApiClient,
    private val chatMessageRepository: ChatMessageRepository,
    private val usageLimitService: UsageLimitService,
    private val failureLogger: MessageDeliveryFailureLogger,
    @Value("\${meta.api.mode:mock}") private val metaApiMode: String,
    @Value("\${meta.api.access-token:}") private val configuredAccessToken: String,
    @Value("\${meta.api.phone-number-id:}") private val configuredPhoneNumberId: String
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun sendText(
        lead: Lead,
        organization: Organization,
        message: String,
        marketplaceName: String? = null,
        senderType: ChatMessageSenderType = ChatMessageSenderType.SELLER,
        botSessionId: UUID? = null,
        intentDetected: String? = null,
        engineUsed: String? = null
    ): ChatMessage {
        checkQuota(organization, marketplaceName)
        val whatsappConfig = resolveWhatsappConfig(organization)

        val waMessageId = try {
            metaApiClient.sendText(
                phoneNumberId = whatsappConfig.phoneNumberId,
                accessToken = whatsappConfig.accessToken,
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
                senderType = senderType,
                content = message,
                messageType = MessageType.TEXT,
                waMessageId = waMessageId,
                status = MessageStatus.SENT,
                botSessionId = botSessionId,
                intentDetected = intentDetected,
                engineUsed = engineUsed
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
        marketplaceName: String? = null,
        senderType: ChatMessageSenderType = ChatMessageSenderType.SELLER
    ): ChatMessage {
        checkQuota(organization, marketplaceName)
        val whatsappConfig = resolveWhatsappConfig(organization)

        val waMessageId = try {
            metaApiClient.sendTemplate(
                phoneNumberId = whatsappConfig.phoneNumberId,
                accessToken = whatsappConfig.accessToken,
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
                senderType = senderType,
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

    private fun resolveWhatsappConfig(organization: Organization): WhatsappConfig {
        val phoneNumberId = organization.whatsappPhoneNumberId ?: configuredPhoneNumberId
        val accessToken = organization.whatsappToken ?: configuredAccessToken
        if (metaApiMode == "mock") {
            return WhatsappConfig(
                phoneNumberId = phoneNumberId.ifBlank { "mock-phone-number-id" },
                accessToken = accessToken.ifBlank { "mock-access-token" }
            )
        }
        require(phoneNumberId.isNotBlank()) {
            "PhoneNumberId não configurado para organização ${organization.id}"
        }
        require(accessToken.isNotBlank()) {
            "WhatsApp token não configurado para organização ${organization.id}"
        }
        return WhatsappConfig(phoneNumberId, accessToken)
    }
}

private data class WhatsappConfig(
    val phoneNumberId: String,
    val accessToken: String
)
