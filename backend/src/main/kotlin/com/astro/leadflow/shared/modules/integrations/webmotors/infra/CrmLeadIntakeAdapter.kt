package com.astro.leadflow.shared.modules.integrations.webmotors.infra

import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.crm.intake.LeadIntakeCommand
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.ingestion.service.LeadIngestionService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.CrmLeadIntake
import com.astro.leadflow.shared.modules.integrations.webmotors.service.IntakeResult
import org.springframework.stereotype.Component

/**
 * Implementação de [CrmLeadIntake]: delega à regra oficial de entrada
 * ([LeadIngestionService.ingestResolved]), a mesma do scraper e do Mercado Livre.
 */
@Component
class CrmLeadIntakeAdapter(
    private val organizationRepository: OrganizationRepository,
    private val leadIngestionService: LeadIngestionService
) : CrmLeadIntake {

    override fun intake(command: LeadIntakeCommand): IntakeResult {
        val organization = organizationRepository.findById(command.organizationId)
            .orElseThrow { ResourceNotFoundException("Organização", command.organizationId) }
        val lead = leadIngestionService.ingestResolved(organization, command)
        return IntakeResult(lead.id, created = true)
    }
}
