package com.astro.leadflow.shared.modules.communication.service

import com.astro.leadflow.shared.modules.communication.api.requests.MetaMessage
import com.astro.leadflow.shared.modules.communication.domain.*
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BotService(
    private val chatMessageRepository: ChatMessageRepository,
    private val leadRepository: LeadRepository,
    private val organizationRepository: OrganizationRepository,
    private val messageSenderService: MessageSenderService
) {

    @Transactional
    fun handleIncoming(phoneNumberId: String, message: MetaMessage, contactName: String?) {
        val organization = organizationRepository.findByWhatsappPhoneNumberId(phoneNumberId)
            ?: return

        val phoneNumber = normalizePhone(message.from)
        val content = message.text?.body ?: return

        val lead = leadRepository
            .findByOrganizationIdAndPhoneNumber(organization.id, phoneNumber)
            .orElseGet {
                leadRepository.save(
                    Lead(
                        organization = organization,
                        phoneNumber = phoneNumber,
                        name = contactName,
                        origin = "WHATSAPP_INBOUND",
                        pipelineStage = PipelineStage.TALKING_TO_BOT
                    )
                )
            }

        chatMessageRepository.save(
            ChatMessage(
                lead = lead,
                organization = organization,
                direction = MessageDirection.INBOUND,
                content = content,
                waMessageId = message.id
            )
        )

        val reply = buildBotReply(lead, content)
        if (reply != null) {
            messageSenderService.sendText(lead, organization, reply)
        }
    }

    private fun buildBotReply(lead: Lead, incomingMessage: String): String? {
        return when (lead.pipelineStage) {
            PipelineStage.TALKING_TO_BOT -> buildQualificationReply(lead, incomingMessage)
            PipelineStage.WAITING_ATTENDANCE -> null
            else -> null
        }
    }

    private fun buildQualificationReply(lead: Lead, message: String): String {
        return when {
            lead.name == null ->
                "Olá! 👋 Bem-vindo à nossa concessionária. Qual o seu nome?"
            lead.vehicleInterest == null ->
                "Prazer, ${lead.name}! Qual veículo você está buscando?"
            lead.interestType == null ->
                "Ótima escolha! Você prefere compra à vista, financiamento ou troca?"
            else ->
                "Perfeito! Um de nossos consultores entrará em contato em breve. 🚗"
        }
    }

    @Transactional
    fun handleStatusUpdate(waMessageId: String, newStatus: String) {
        val message = chatMessageRepository.findByWaMessageId(waMessageId) ?: return
        message.status = when (newStatus) {
            "delivered" -> MessageStatus.DELIVERED
            "read" -> MessageStatus.READ
            "failed" -> MessageStatus.FAILED
            else -> return
        }
        chatMessageRepository.save(message)
    }

    private fun normalizePhone(phone: String): String =
        "+${phone.trimStart('+')}"
}