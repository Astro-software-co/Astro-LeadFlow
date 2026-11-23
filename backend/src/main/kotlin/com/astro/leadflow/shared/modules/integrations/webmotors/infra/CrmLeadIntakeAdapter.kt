package com.astro.leadflow.shared.modules.integrations.webmotors.infra

import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.communication.service.MessageSenderService
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.integrations.webmotors.service.CrmLeadIntake
import com.astro.leadflow.shared.modules.integrations.webmotors.service.IncomingLead
import com.astro.leadflow.shared.modules.integrations.webmotors.service.IntakeResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

/**
 * Implementação de [CrmLeadIntake] sobre o CRM existente. É o único ponto do módulo Webmotors
 * que conhece [Lead].
 *
 * - Telefone já existente na organização: não duplica; acrescenta o novo interesse às notas.
 * - Lead novo: entra em TALKING_TO_BOT só se a mensagem de boas-vindas for enviada; caso
 *   contrário vai direto para WAITING_ATTENDANCE (não há bot conversando).
 * - A mensagem de boas-vindas é enviada fora da transação e a falha dela não desfaz o lead.
 */
@Component
class CrmLeadIntakeAdapter(
    private val leadRepository: LeadRepository,
    private val organizationRepository: OrganizationRepository,
    private val messageSenderService: MessageSenderService,
    transactionManager: PlatformTransactionManager
) : CrmLeadIntake {

    private val log = LoggerFactory.getLogger(javaClass)
    private val tx = TransactionTemplate(transactionManager)

    override fun intake(organizationId: UUID, lead: IncomingLead): IntakeResult {
        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { ResourceNotFoundException("Organização", organizationId) }

        val (leadId, created) = tx.execute {
            val existing = leadRepository.findFirstByOrganizationIdAndPhoneNumberOrderByCreatedAtAsc(organizationId, lead.phone).orElse(null)
            if (existing != null) {
                existing.notes = listOfNotNull(
                    existing.notes,
                    "— Novo interesse via ${lead.origin} —\n${lead.notes.orEmpty()}".trim()
                ).joinToString("\n\n")
                leadRepository.save(existing)
                existing.id to false
            } else {
                val saved = leadRepository.save(
                    Lead(
                        organization = organization,
                        phoneNumber = lead.phone,
                        name = lead.name,
                        email = lead.email,
                        origin = lead.origin,
                        vehicleInterest = lead.vehicleInterest,
                        notes = lead.notes,
                        pipelineStage = if (lead.sendWelcomeMessage) PipelineStage.TALKING_TO_BOT
                        else PipelineStage.WAITING_ATTENDANCE
                    )
                )
                saved.id to true
            }
        }!!

        if (created && lead.sendWelcomeMessage) {
            try {
                val saved = leadRepository.findById(leadId).orElseThrow()
                messageSenderService.sendTemplate(saved, organization, "welcome_lead", lead.origin)
            } catch (e: Exception) {
                log.error("Falha ao enviar boas-vindas ao lead {}: {}", leadId, e.message)
            }
        }
        return IntakeResult(leadId, created)
    }
}
