package com.astro.leadflow.modules.communication.service

import com.astro.leadflow.modules.communication.api.requests.MetaMessage
import com.astro.leadflow.modules.communication.domain.*
import com.astro.leadflow.modules.crm.domain.*
import com.astro.leadflow.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BotService(
    private val chatMessageRepository: ChatMessageRepository,
    private val leadRepository: LeadRepository,
    private val organizationRepository: OrganizationRepository,
    private val messageSenderService: MessageSenderService
) {

    /**
     * Ponto de entrada para toda mensagem recebida via webhook da Meta.
     * Orquestra: localizar lead → registrar mensagem → responder via bot.
     */
    @Transactional
    fun handleIncoming(phoneNumberId: String, message: MetaMessage, contactName: String?) {
        val organization = organizationRepository.findByWhatsappPhoneNumberId(phoneNumberId)
            ?: return // Organização não encontrada para esse número — ignora

        val phoneNumber = normalizePhone(message.from)
        val content = message.text?.body ?: return // Ignora mensagens sem texto por ora

        // Localiza ou cria lead
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

        // Registra mensagem recebida
        chatMessageRepository.save(
            ChatMessage(
                lead = lead,
                organization = organization,
                direction = MessageDirection.INBOUND,
                content = content,
                waMessageId = message.id
            )
        )

        // Processa resposta do bot baseada no estágio atual
        val reply = buildBotReply(lead, content)

        if (reply != null) {
            messageSenderService.sendText(lead, organization, reply)
        }
    }

    /**
     * Lógica de resposta do bot por estágio.
     * Ponto de extensão para Amazon Lex / Bedrock no futuro.
     */
    private fun buildBotReply(lead: Lead, incomingMessage: String): String? {
        return when (lead.pipelineStage) {
            PipelineStage.TALKING_TO_BOT -> buildQualificationReply(lead, incomingMessage)
            PipelineStage.WAITING_ATTENDANCE -> null // Humano assume — bot silencia
            else -> null
        }
    }

    private fun buildQualificationReply(lead: Lead, message: String): String {
        // Fluxo básico de qualificação — extensível para Lex/Bedrock
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

    /**
     * Atualiza status de entrega de mensagem (delivered, read, failed).
     */
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