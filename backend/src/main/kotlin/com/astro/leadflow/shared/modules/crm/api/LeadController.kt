package com.astro.leadflow.modules.crm.api

import com.astro.leadflow.modules.crm.api.requests.*
import com.astro.leadflow.modules.crm.api.responses.LeadResponse
import com.astro.leadflow.modules.crm.service.LeadAssignmentService
import com.astro.leadflow.modules.crm.service.LeadService
import com.astro.leadflow.modules.crm.service.PipelineService
import com.astro.leadflow.modules.crm.service.StageHistoryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/leads")
@Tag(name = "CRM — Leads", description = "Gestão de leads e Kanban")
class LeadController(
    private val leadService: LeadService,
    private val pipelineService: PipelineService,
    private val assignmentService: LeadAssignmentService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria lead manualmente")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    fun create(@Valid @RequestBody request: LeadCreateRequest): LeadResponse =
        leadService.create(request)

    @GetMapping("/{id}")
    @Operation(summary = "Busca lead por ID")
    fun findById(@PathVariable id: UUID): LeadResponse =
        leadService.findById(id)

    @GetMapping
    @Operation(summary = "Lista todos os leads da organização")
    fun findAll(): List<LeadResponse> =
        leadService.findAll()

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados do lead")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: LeadUpdateRequest
    ): LeadResponse = leadService.update(id, request)

    @PatchMapping("/{id}/move")
    @Operation(summary = "Move lead para outro estágio do Kanban")
    fun move(
        @PathVariable id: UUID,
        @Valid @RequestBody request: MoveLeadRequest
    ): LeadResponse = pipelineService.moveLead(id, request.targetStage)

    @PatchMapping("/{id}/advance")
    @Operation(summary = "Avança lead para o próximo estágio")
    fun advance(@PathVariable id: UUID): LeadResponse =
        pipelineService.advanceLead(id)

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Arquiva lead")
    fun archive(@PathVariable id: UUID) =
        leadService.archive(id)

    @PatchMapping("/{id}/assume")
    @Operation(summary = "Vendedor assume o lead")
    fun assume(@PathVariable id: UUID): LeadResponse =
        assignmentService.assumeLead(id)

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Admin/Manager atribui lead a um vendedor")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    fun assign(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AssignLeadRequest
    ): LeadResponse = assignmentService.assignTo(id, request.sellerId)

    @GetMapping("/{id}/history")
    @Operation(summary = "Histórico de movimentações do lead")
    fun history(@PathVariable id: UUID): List<StageHistoryResponse> =
        pipelineService.getLeadHistory(id)
}