package com.astro.leadflow.shared.modules.inventory.api

import com.astro.leadflow.shared.modules.inventory.api.requests.VehicleCreateRequest
import com.astro.leadflow.shared.modules.inventory.api.requests.VehicleUpdateRequest
import com.astro.leadflow.shared.modules.inventory.api.responses.VehicleResponse
import com.astro.leadflow.shared.modules.inventory.api.responses.VehicleStockSummary
import com.astro.leadflow.shared.modules.inventory.domain.VehicleStatus
import com.astro.leadflow.shared.modules.inventory.service.VehicleService
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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Inventory — Veículos", description = "Gestão de estoque de veículos")
class VehicleController(private val vehicleService: VehicleService) {
    companion object {
        private const val DEFAULT_PAGE = 0
        private const val DEFAULT_SIZE = 20
        private const val MAX_SIZE = 100
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra novo veículo no estoque")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Veículo cadastrado com sucesso"),
        ApiResponse(responseCode = "400", description = "Dados inválidos"),
        ApiResponse(responseCode = "403", description = "Sem permissão")
    )
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    fun create(@Valid @RequestBody request: VehicleCreateRequest): VehicleResponse =
        vehicleService.create(request)

    @GetMapping
    @Operation(
        summary = "Lista veículos do estoque",
        description = "Suporta filtro por texto (marca/modelo), status e paginação"
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    fun findAll(
        @Parameter(description = "Termo de busca por marca ou modelo")
        @RequestParam(required = false) search: String?,
        @Parameter(description = "Filtrar por status")
        @RequestParam(required = false) status: VehicleStatus?,
        @Parameter(description = "Página (inicia em 0)")
        @RequestParam(required = false, defaultValue = "$DEFAULT_PAGE") page: Int,
        @Parameter(description = "Itens por página (máx. 100)")
        @RequestParam(required = false, defaultValue = "$DEFAULT_SIZE") size: Int
    ): Page<VehicleResponse> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, MAX_SIZE)
        val pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"))

        return vehicleService.findAll(search, status, pageable)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca veículo por ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Veículo encontrado"),
        ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    )
    fun findById(
        @Parameter(description = "ID do veículo") @PathVariable id: UUID
    ): VehicleResponse = vehicleService.findById(id)

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados do veículo")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Veículo atualizado"),
        ApiResponse(responseCode = "404", description = "Veículo não encontrado"),
        ApiResponse(responseCode = "422", description = "Operação inválida — ex: desfazer venda")
    )
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: VehicleUpdateRequest
    ): VehicleResponse = vehicleService.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove veículo do estoque")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Veículo removido"),
        ApiResponse(responseCode = "422", description = "Veículo reservado ou vendido não pode ser removido")
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable id: UUID) = vehicleService.delete(id)

    @GetMapping("/summary")
    @Operation(summary = "Resumo quantitativo do estoque por status")
    @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso")
    fun summary(): VehicleStockSummary = vehicleService.getStockSummary()
}
