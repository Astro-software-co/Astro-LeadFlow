package com.astro.leadflow.shared.modules.crm.service

import com.astro.leadflow.shared.modules.crm.api.requests.AssignLeadRequest
import com.astro.leadflow.shared.modules.crm.api.requests.MoveLeadRequest
import com.astro.leadflow.shared.modules.crm.api.responses.KanbanBoardResponse
import com.astro.leadflow.shared.modules.crm.api.responses.LeadResponse
import com.astro.leadflow.shared.modules.crm.api.responses.StageHistoryResponse
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.LeadStageHistory
import com.astro.leadflow.shared.modules.crm.domain.LeadStageHistoryRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.modules.crm.mapper.LeadMapper
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.exception.AccessDeniedException
import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.util.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PipelineService(
    private val leadRepository: LeadRepository,
    private val leadStageHistoryRepository: LeadStageHistoryRepository,
    private val userRepository: UserRepository,
    private val leadMapper: LeadMapper
) {

    @Transactional(readOnly = true)
    fun getBoard(): KanbanBoardResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val leads = leadRepository.findBoardByOrganization(orgId)

        val stages = PipelineStage.entries
            .filter { it != PipelineStage.ARCHIVED }
            .associateWith { stage ->
                leads
                    .filter { lead -> lead.pipelineStage == stage }
                    .map { lead -> leadMapper.toResponse(lead) }
            }

        return KanbanBoardResponse(stages = stages)
    }

    // Chamado pelo controller (recebe request)
    @Transactional
    fun moveLead(leadId: UUID, request: MoveLeadRequest): LeadResponse =
        moveLead(leadId, request.toStage)

    // Overload interno — chamado por LeadAssignmentService e testes
    @Transactional
    fun moveLead(leadId: UUID, toStage: PipelineStage): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val lead = leadRepository.findByOrganizationIdAndId(orgId, leadId)
            .orElseThrow { ResourceNotFoundException("Lead", leadId) }

        val fromStage = lead.pipelineStage

        if (fromStage == PipelineStage.ARCHIVED)
            throw InvalidOperationException("Lead arquivado não pode ser movido.")

        if (fromStage == toStage)
            throw InvalidOperationException("Lead já está no stage ${toStage.displayName}.")

        if (toStage == PipelineStage.ARCHIVED)
            throw InvalidOperationException("Use o endpoint de arquivamento para arquivar leads.")

        val currentUser = SecurityUtils.getCurrentUser()

        leadStageHistoryRepository.save(
            LeadStageHistory(
                lead = lead,
                fromStage = fromStage,
                toStage = toStage,
                movedBy = currentUser
            )
        )

        lead.pipelineStage = toStage
        return leadMapper.toResponse(leadRepository.save(lead))
    }

    @Transactional
    fun advanceLead(leadId: UUID): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val lead = leadRepository.findByOrganizationIdAndId(orgId, leadId)
            .orElseThrow { ResourceNotFoundException("Lead", leadId) }

        val nextStage = lead.pipelineStage.next()
            ?: throw InvalidOperationException("Lead já está no estágio final.")

        return moveLead(leadId, nextStage)
    }

    @Transactional
    fun assignLead(leadId: UUID, request: AssignLeadRequest): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val lead = leadRepository.findByOrganizationIdAndId(orgId, leadId)
            .orElseThrow { ResourceNotFoundException("Lead", leadId) }

        val assignee = userRepository.findById(request.userId)
            .orElseThrow { ResourceNotFoundException("User", request.userId) }

        if (!assignee.belongsToOrganization(orgId))
            throw AccessDeniedException("Usuário não pertence a esta organização.")

        lead.assignedTo = assignee
        return leadMapper.toResponse(leadRepository.save(lead))
    }

    @Transactional
    fun unassignLead(leadId: UUID): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val lead = leadRepository.findByOrganizationIdAndId(orgId, leadId)
            .orElseThrow { ResourceNotFoundException("Lead", leadId) }

        lead.assignedTo = null
        return leadMapper.toResponse(leadRepository.save(lead))
    }

    @Transactional(readOnly = true)
    fun getLeadsByStage(stage: PipelineStage): List<LeadResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return leadRepository.findAllByOrganizationIdAndPipelineStage(orgId, stage)
            .map { lead -> leadMapper.toResponse(lead) }
    }

    @Transactional(readOnly = true)
    fun getLeadActivities(leadId: UUID): List<StageHistoryResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val lead = leadRepository.findByOrganizationIdAndId(orgId, leadId)
            .orElseThrow { ResourceNotFoundException("Lead", leadId) }

        return leadStageHistoryRepository.findAllByLeadIdOrderByMovedAtAsc(lead.id)
            .map { history -> leadMapper.toHistory(history) }
    }
}
