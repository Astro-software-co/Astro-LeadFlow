package com.astro.leadflow.modules.crm.service

import com.astro.leadflow.modules.crm.api.responses.KanbanBoardResponse
import com.astro.leadflow.modules.crm.api.responses.LeadResponse
import com.astro.leadflow.modules.crm.api.responses.StageHistoryResponse
import com.astro.leadflow.modules.crm.domain.*
import com.astro.leadflow.modules.crm.mapper.LeadMapper
import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.util.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PipelineService(
    private val leadRepository: LeadRepository,
    private val historyRepository: LeadStageHistoryRepository,
    private val leadService: LeadService,
    private val leadMapper: LeadMapper
) {

    @Transactional(readOnly = true)
    fun getBoard(): KanbanBoardResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val leads = leadRepository.findBoardByOrganization(orgId)
        val grouped = PipelineStage.entries
            .filter { it != PipelineStage.ARCHIVED }
            .associateWith { stage ->
                leads.filter { it.pipelineStage == stage }
                    .map { leadMapper.toResponse(it) }
            }
        return KanbanBoardResponse(stages = grouped)
    }

    @Transactional
    fun moveLead(leadId: UUID, targetStage: PipelineStage): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val currentUser = SecurityUtils.getCurrentUser()
        val lead = leadService.findLeadInOrg(leadId, orgId)

        if (lead.pipelineStage == PipelineStage.ARCHIVED)
            throw InvalidOperationException("Leads arquivados não podem ser movidos.")

        if (lead.pipelineStage == targetStage)
            throw InvalidOperationException(
                "Lead já está no estágio '${targetStage.displayName}'."
            )

        val fromStage = lead.pipelineStage
        lead.pipelineStage = targetStage
        leadRepository.save(lead)

        historyRepository.save(
            LeadStageHistory(
                lead = lead,
                movedBy = currentUser,
                fromStage = fromStage,
                toStage = targetStage
            )
        )
        return leadMapper.toResponse(lead)
    }

    @Transactional
    fun advanceLead(leadId: UUID): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val lead = leadService.findLeadInOrg(leadId, orgId)
        val nextStage = lead.pipelineStage.next()
            ?: throw InvalidOperationException(
                "Lead já está no estágio final '${lead.pipelineStage.displayName}'."
            )
        return moveLead(leadId, nextStage)
    }

    @Transactional(readOnly = true)
    fun getLeadHistory(leadId: UUID): List<StageHistoryResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        leadService.findLeadInOrg(leadId, orgId)
        return historyRepository.findAllByLeadIdOrderByMovedAtDesc(leadId)
            .map { leadMapper.toHistory(it) }
    }
}