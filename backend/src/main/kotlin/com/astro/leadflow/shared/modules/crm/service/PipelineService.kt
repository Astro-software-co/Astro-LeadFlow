package com.astro.leadflow.modules.crm.service

import com.astro.leadflow.modules.crm.api.responses.KanbanBoardResponse
import com.astro.leadflow.modules.crm.api.responses.LeadResponse
import com.astro.leadflow.modules.crm.domain.*
import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.util.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PipelineService(
    private val leadRepository: LeadRepository,
    private val historyRepository: LeadStageHistoryRepository,
    private val leadService: LeadService
) {

    @Transactional(readOnly = true)
    fun getBoard(): KanbanBoardResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val leads = leadRepository.findBoardByOrganization(orgId)

        // Agrupa por stage mantendo a ordem definida no enum
        val grouped = PipelineStage.entries
            .filter { it != PipelineStage.ARCHIVED }
            .associateWith { stage ->
                leads.filter { it.pipelineStage == stage }.map { LeadResponse.from(it) }
            }

        return KanbanBoardResponse(stages = grouped)
    }

    @Transactional
    fun moveLead(leadId: UUID, targetStage: PipelineStage): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val currentUser = SecurityUtils.getCurrentUser()
        val lead = leadService.findLeadInOrg(leadId, orgId)

        if (lead.pipelineStage == targetStage) {
            throw BusinessException("Lead já está no estágio '${targetStage.displayName}'.")
        }

        val fromStage = lead.pipelineStage
        lead.pipelineStage = targetStage
        leadRepository.save(lead)

        // Persiste histórico de movimentação
        historyRepository.save(
            LeadStageHistory(
                lead = lead,
                movedBy = currentUser,
                fromStage = fromStage,
                toStage = targetStage
            )
        )

        return LeadResponse.from(lead)
    }

    @Transactional
    fun advanceLead(leadId: UUID): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val lead = leadService.findLeadInOrg(leadId, orgId)

        val nextStage = lead.pipelineStage.next()
            ?: throw BusinessException("Lead já está no estágio final.")

        return moveLead(leadId, nextStage)
    }

    @Transactional(readOnly = true)
    fun getLeadHistory(leadId: UUID): List<StageHistoryResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        leadService.findLeadInOrg(leadId, orgId) // valida acesso
        return historyRepository.findAllByLeadIdOrderByMovedAtDesc(leadId)
            .map { StageHistoryResponse.from(it) }
    }
}

data class StageHistoryResponse(
    val fromStage: PipelineStage?,
    val toStage: PipelineStage,
    val movedBy: String?,
    val movedAt: java.time.LocalDateTime
) {
    companion object {
        fun from(h: LeadStageHistory) = StageHistoryResponse(
            fromStage = h.fromStage,
            toStage = h.toStage,
            movedBy = h.movedBy?.name,
            movedAt = h.movedAt
        )
    }
}