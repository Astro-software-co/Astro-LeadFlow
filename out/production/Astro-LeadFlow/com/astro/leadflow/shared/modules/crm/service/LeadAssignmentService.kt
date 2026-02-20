package com.astro.leadflow.modules.crm.service

import com.astro.leadflow.modules.crm.api.responses.LeadResponse
import com.astro.leadflow.modules.crm.domain.LeadRepository
import com.astro.leadflow.modules.crm.domain.PipelineStage
import com.astro.leadflow.modules.iam.domain.Role
import com.astro.leadflow.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.util.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LeadAssignmentService(
    private val leadRepository: LeadRepository,
    private val userRepository: UserRepository,
    private val leadService: LeadService,
    private val pipelineService: PipelineService
) {

    /**
     * Vendedor assume o lead para si mesmo.
     * Move automaticamente para WAITING_ATTENDANCE se ainda estiver em TALKING_TO_BOT.
     */
    @Transactional
    fun assumeLead(leadId: UUID): LeadResponse {
        val currentUser = SecurityUtils.getCurrentUser()
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val lead = leadService.findLeadInOrg(leadId, orgId)

        if (lead.assignedTo != null && lead.assignedTo!!.id == currentUser.id) {
            throw BusinessException("Você já é o responsável por este lead.")
        }

        lead.assignedTo = currentUser
        leadRepository.save(lead)

        // Avança automaticamente se ainda estiver no bot
        if (lead.pipelineStage == PipelineStage.TALKING_TO_BOT) {
            return pipelineService.moveLead(leadId, PipelineStage.WAITING_ATTENDANCE)
        }

        return LeadResponse.from(lead)
    }

    /**
     * Admin/Manager atribui lead a um vendedor específico.
     */
    @Transactional
    fun assignTo(leadId: UUID, sellerId: UUID): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val lead = leadService.findLeadInOrg(leadId, orgId)

        val seller = userRepository.findById(sellerId)
            .orElseThrow { ResourceNotFoundException("Seller", sellerId) }

        if (!seller.belongsToOrganization(orgId)) {
            throw BusinessException("Vendedor não pertence a esta organização.")
        }

        if (seller.role !in listOf(Role.SELLER, Role.MANAGER)) {
            throw BusinessException("Usuário não pode receber leads.")
        }

        lead.assignedTo = seller
        leadRepository.save(lead)

        return LeadResponse.from(lead)
    }
}