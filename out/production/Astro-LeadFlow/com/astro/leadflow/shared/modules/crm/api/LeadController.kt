package com.astro.leadflow.modules.crm.api

import com.astro.leadflow.modules.crm.api.requests.*
import com.astro.leadflow.modules.crm.api.responses.*
import com.astro.leadflow.modules.crm.service.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/leads")
@Tag(name = "CRM — Leads", description = "Gestão completa de leads e pipeline")
class LeadController(
    private val leadService: LeadService,
    private val pipelineService: PipelineService,
    private val assignmentService: LeadAssignmentService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria lead manualmente")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Lead criado com sucesso"),
        ApiResponse(responseCode = "400", description = "Dados inválidos"),
        ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    )
    fun create(@Valid @RequestBody request: LeadCreateRequest): LeadResponse =
        leadService.create(request)

    @GetMapping
    @Operation(summary = "Lista todos os leads da organização")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    fun findAll(): List<LeadResponse> = leadService.findAll()

    @GetMapping("/{id}")
    @Operation(summary = "Busca lead por ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lead encontrado"),
        ApiResponse(responseCode = "404", description = "Lead não encontrado")
    )
    fun findById(
        @Parameter(description = "ID do lead") @PathVariable id: UUID
    ): LeadResponse = leadService.findById(id)

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados do lead")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lead atualizado"),
        ApiResponse(responseCode = "404", description = "Lead ou veículo não encontrado")
    )
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: LeadUpdateRequest
    ): LeadResponse = leadService.update(id, request)

    @PatchMapping("/{id}/move")
    @Operation(summary = "Move lead para estágio específico do Kanban")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lead movido com sucesso"),
        ApiResponse(responseCode = "422", description = "Operação inválida — lead já está no estágio")
    )
    fun move(
        @PathVariable id: UUID,
        @Valid @RequestBody request: MoveLeadRequest
    ): LeadResponse = pipelineService.moveLead(id, request.targetStage)

    @PatchMapping("/{id}/advance")
    @Operation(summary = "Avança lead para o próximo estágio automaticamente")
    @ApiResponse(responseCode = "422", description = "Lead já está no estágio final")
    fun advance(@PathVariable id: UUID): LeadResponse =
        pipelineService.advanceLead(id)

    @PatchMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Arquiva lead")
    @ApiResponse(responseCode = "422", description = "Lead já está arquivado")
    fun archive(@PathVariable id: UUID) =
        leadService.archive(id)

    @PatchMapping("/{id}/assume")
    @Operation(summary = "Vendedor assume o lead e avança para Aguardando Atendimento")
    fun assume(@PathVariable id: UUID): LeadResponse =
        assignmentService.assumeLead(id)

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Admin ou Manager atribui lead a um vendedor")
    @ApiResponse(responseCode = "403", description = "Sem permissão para atribuir leads")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    fun assign(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AssignLeadRequest
    ): LeadResponse = assignmentService.assignTo(id, request.sellerId)

    @GetMapping("/{id}/history")
    @Operation(summary = "Histórico de movimentações do lead no Kanban")
    fun history(@PathVariable id: UUID): List<StageHistoryResponse> =
        pipelineService.getLeadHistory(id)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove lead permanentemente")
    @ApiResponse(responseCode = "204", description = "Lead removido com sucesso")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    fun delete(@PathVariable id: UUID) =
        leadService.delete(id)
}