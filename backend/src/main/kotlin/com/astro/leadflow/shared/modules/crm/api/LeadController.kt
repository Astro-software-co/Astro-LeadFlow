package com.astro.leadflow.shared.modules.crm.api

import com.astro.leadflow.shared.modules.crm.api.requests.AssignLeadRequest
import com.astro.leadflow.shared.modules.crm.api.requests.LeadCreateRequest
import com.astro.leadflow.shared.modules.crm.api.requests.LeadUpdateRequest
import com.astro.leadflow.shared.modules.crm.api.requests.MoveLeadRequest
import com.astro.leadflow.shared.modules.crm.api.responses.LeadResponse
import com.astro.leadflow.shared.modules.crm.api.responses.StageHistoryResponse
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.modules.crm.service.LeadAssignmentService
import com.astro.leadflow.shared.modules.crm.service.LeadService
import com.astro.leadflow.shared.modules.crm.service.PipelineService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/leads")
@Tag(name = "CRM — Leads", description = "Gestão completa de leads e pipeline")
class LeadController(
    private val leadService: LeadService,
    private val pipelineService: PipelineService,
    private val assignmentService: LeadAssignmentService
) {
    companion object {
        private const val DEFAULT_PAGE = 0
        private const val DEFAULT_SIZE = 20
        private const val MAX_SIZE = 100
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria lead manualmente")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Lead criado com sucesso"),
        ApiResponse(responseCode = "400", description = "Dados inválidos"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "403", description = "Sem permissão"),
        ApiResponse(responseCode = "404", description = "Veículo não encontrado"),
        ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    )
    fun create(@Valid @RequestBody request: LeadCreateRequest): LeadResponse =
        leadService.create(request)

    @GetMapping
    @Operation(summary = "Lista leads da organização com filtros e paginação")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "403", description = "Sem permissão"),
        ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    )
    fun findAll(
        @Parameter(description = "Busca por nome, telefone ou e-mail")
        @RequestParam(required = false) search: String?,
        @Parameter(description = "Filtrar por origem")
        @RequestParam(required = false) origin: String?,
        @Parameter(description = "Filtrar por estágio do pipeline")
        @RequestParam(required = false) stage: PipelineStage?,
        @Parameter(description = "Filtrar por responsável (UUID)")
        @RequestParam(required = false) assignedTo: UUID?,
        @Parameter(description = "Data inicial de criação (yyyy-MM-dd)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        dateFrom: LocalDate?,
        @Parameter(description = "Data final de criação (yyyy-MM-dd)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        dateTo: LocalDate?,
        @Parameter(description = "Página (inicia em 0)")
        @RequestParam(required = false, defaultValue = "$DEFAULT_PAGE") page: Int,
        @Parameter(description = "Itens por página (máx. 100)")
        @RequestParam(required = false, defaultValue = "$DEFAULT_SIZE") size: Int
    ): Page<LeadResponse> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, MAX_SIZE)
        val pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"))

        return leadService.findAll(search, origin, stage, assignedTo, dateFrom, dateTo, pageable)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca lead por ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lead encontrado"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "403", description = "Sem permissão"),
        ApiResponse(responseCode = "404", description = "Lead não encontrado"),
        ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    )
    fun findById(
        @Parameter(description = "ID do lead") @PathVariable id: UUID
    ): LeadResponse = leadService.findById(id)

    @GetMapping("/{id}/activities")
    @Operation(summary = "Lista timeline de movimentações do lead")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Timeline retornada com sucesso"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "403", description = "Sem permissão"),
        ApiResponse(responseCode = "404", description = "Lead não encontrado"),
        ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    )
    fun findActivities(
        @Parameter(description = "ID do lead") @PathVariable id: UUID
    ): List<StageHistoryResponse> = pipelineService.getLeadActivities(id)

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados do lead")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lead atualizado"),
        ApiResponse(responseCode = "400", description = "Dados inválidos"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "403", description = "Sem permissão"),
        ApiResponse(responseCode = "404", description = "Lead ou veículo não encontrado"),
        ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    )
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: LeadUpdateRequest
    ): LeadResponse = leadService.update(id, request)

    @PatchMapping("/{id}/move")
    @Operation(summary = "Move lead para estágio específico do Kanban")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lead movido com sucesso"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "403", description = "Sem permissão"),
        ApiResponse(responseCode = "404", description = "Lead não encontrado"),
        ApiResponse(responseCode = "422", description = "Lead já está no estágio ou arquivado"),
        ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    )
    fun move(
        @PathVariable id: UUID,
        @Valid @RequestBody request: MoveLeadRequest
    ): LeadResponse = pipelineService.moveLead(id, request)

    @PatchMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Arquiva lead")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Lead arquivado com sucesso"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "403", description = "Sem permissão"),
        ApiResponse(responseCode = "404", description = "Lead não encontrado"),
        ApiResponse(responseCode = "422", description = "Lead já está arquivado"),
        ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    )
    fun archive(@PathVariable id: UUID) =
        leadService.archive(id)

    @PatchMapping("/{id}/assume")
    @Operation(summary = "Vendedor assume o lead")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lead assumido com sucesso"),
        ApiResponse(responseCode = "400", description = "Você já é o responsável por este lead"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "403", description = "Sem permissão"),
        ApiResponse(responseCode = "404", description = "Lead não encontrado"),
        ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    )
    fun assume(@PathVariable id: UUID): LeadResponse =
        assignmentService.assumeLead(id)

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Manager atribui lead a um vendedor")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lead atribuído com sucesso"),
        ApiResponse(responseCode = "400", description = "Vendedor inválido ou não pertence à organização"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "403", description = "Sem permissão para atribuir leads"),
        ApiResponse(responseCode = "404", description = "Lead não encontrado"),
        ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    )
    @PreAuthorize("hasRole('MANAGER')")
    fun assign(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AssignLeadRequest
    ): LeadResponse = assignmentService.assignTo(id, request.userId)
}
