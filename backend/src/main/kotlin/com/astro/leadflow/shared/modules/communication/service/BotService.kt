package com.astro.leadflow.shared.modules.communication.service

import com.astro.leadflow.shared.modules.communication.api.requests.MetaMessage
import com.astro.leadflow.shared.modules.communication.domain.*
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BotService(
    private val chatMessageRepository: ChatMessageRepository,
    private val leadRepository: LeadRepository,
    private val organizationRepository: OrganizationRepository,
    private val messageSenderService: MessageSenderService
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun handleIncoming(phoneNumberId: String, message: MetaMessage, contactName: String?) {
        val organization = organizationRepository.findByWhatsappPhoneNumberId(phoneNumberId)
        if (organization == null) {
            log.warn("Organização não encontrada para phoneNumberId={}", phoneNumberId)
            return
        }

        val phoneNumber = normalizePhone(message.from)
        val content = message.text?.body
        if (content == null) {
            log.debug("Mensagem sem conteúdo de texto ignorada. type={} from={}", message.type, message.from)
            return
        }

        val lead = leadRepository
            .findByOrganizationIdAndPhoneNumber(organization.id, phoneNumber)
            .orElseGet {
                log.info("Novo lead inbound criado. phone={} org={}", phoneNumber, organization.id)
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
                waMessageId = message.id,
                status = MessageStatus.DELIVERED
            )
        )

        val reply = buildBotReply(lead, content)
        if (reply != null) {
            try {
                messageSenderService.sendText(lead, organization, reply)
            } catch (e: Exception) {
                log.error("Falha ao enviar resposta automática para lead={}. error={}", lead.id, e.message)
            }
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
            lead.name == null -> {
                lead.name = message.trim()
                leadRepository.save(lead)
                "Prazer, ${lead.name}! Qual veículo você está buscando?"
            }
            lead.vehicleInterest == null -> {
                lead.vehicleInterest = message.trim()
                leadRepository.save(lead)
                "Ótima escolha! Você prefere compra à vista, financiamento ou troca?"
            }
            lead.interestType == null -> {
                lead.pipelineStage = PipelineStage.WAITING_ATTENDANCE
                leadRepository.save(lead)
                "Perfeito! Um de nossos consultores entrará em contato em breve. 🚗"
            }
            else -> {
                "Um de nossos consultores entrará em contato em breve. 🚗"
            }
        }
    }

    @Transactional
    fun handleStatusUpdate(waMessageId: String, newStatus: String) {
        val status = when (newStatus) {
            "sent" -> MessageStatus.SENT
            "delivered" -> MessageStatus.DELIVERED
            "read" -> MessageStatus.READ
            "failed" -> MessageStatus.FAILED
            else -> {
                log.debug("Status desconhecido ignorado: {} para waMessageId={}", newStatus, waMessageId)
                return
            }
        }

        val message = chatMessageRepository.findByWaMessageId(waMessageId)
        if (message == null) {
            log.debug("ChatMessage não encontrada para waMessageId={}", waMessageId)
            return
        }

        if (shouldUpdateStatus(message.status, status)) {
            message.status = status
            chatMessageRepository.save(message)
            log.debug("Status atualizado: waMessageId={} {} -> {}", waMessageId, message.status, status)
        }
    }

    private fun shouldUpdateStatus(current: MessageStatus, incoming: MessageStatus): Boolean {
        val order = mapOf(
            MessageStatus.SENT to 0,
            MessageStatus.DELIVERED to 1,
            MessageStatus.READ to 2,
            MessageStatus.FAILED to 3
        )
        return (order[incoming] ?: 0) > (order[current] ?: 0)
    }

    private fun normalizePhone(phone: String): String =
        "+${phone.trimStart('+')}"
}