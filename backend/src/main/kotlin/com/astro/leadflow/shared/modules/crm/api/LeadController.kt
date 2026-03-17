package com.astro.leadflow.shared.modules.crm.api

import com.astro.leadflow.shared.modules.crm.api.requests.AssignLeadRequest
import com.astro.leadflow.shared.modules.crm.api.requests.LeadCreateRequest
import com.astro.leadflow.shared.modules.crm.api.requests.LeadUpdateRequest
import com.astro.leadflow.shared.modules.crm.api.requests.MoveLeadRequest
import com.astro.leadflow.shared.modules.crm.api.responses.LeadResponse
import com.astro.leadflow.shared.modules.crm.api.responses.StageHistoryResponse
import com.astro.leadflow.shared.modules.crm.service.LeadAssignmentService
import com.astro.leadflow.shared.modules.crm.service.LeadService
import com.astro.leadflow.shared.modules.crm.service.PipelineService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
        ApiResponse(responseCode = "422", description = "Lead já está no estágio ou arquivado")
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
        ApiResponse(responseCode = "422", description = "Lead já está arquivado")
    )
    fun archive(@PathVariable id: UUID) =
        leadService.archive(id)

    @PatchMapping("/{id}/assume")
    @Operation(summary = "Vendedor assume o lead")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lead assumido com sucesso"),
        ApiResponse(responseCode = "400", description = "Você já é o responsável por este lead")
    )
    fun assume(@PathVariable id: UUID): LeadResponse =
        assignmentService.assumeLead(id)

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Admin ou Manager atribui lead a um vendedor")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lead atribuído com sucesso"),
        ApiResponse(responseCode = "400", description = "Vendedor inválido ou não pertence à organização"),
        ApiResponse(responseCode = "403", description = "Sem permissão para atribuir leads")
    )
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    fun assign(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AssignLeadRequest
    ): LeadResponse = assignmentService.assignTo(id, request.userId)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove lead permanentemente")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Lead removido com sucesso"),
        ApiResponse(responseCode = "403", description = "Sem permissão para remover leads")
    )
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    fun delete(@PathVariable id: UUID) =
        leadService.delete(id)
}