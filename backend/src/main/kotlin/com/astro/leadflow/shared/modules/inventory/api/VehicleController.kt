package com.astro.leadflow.modules.inventory.api

import com.astro.leadflow.modules.inventory.api.requests.VehicleCreateRequest
import com.astro.leadflow.modules.inventory.api.requests.VehicleUpdateRequest
import com.astro.leadflow.modules.inventory.api.responses.VehicleResponse
import com.astro.leadflow.modules.inventory.api.responses.VehicleStockSummary
import com.astro.leadflow.modules.inventory.domain.VehicleStatus
import com.astro.leadflow.modules.inventory.service.VehicleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Inventory — Veículos", description = "Gestão de estoque de veículos")
class VehicleController(
    private val vehicleService: VehicleService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra novo veículo")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    fun create(@Valid @RequestBody request: VehicleCreateRequest): VehicleResponse =
        vehicleService.create(request)

    @GetMapping
    @Operation(summary = "Lista veículos com filtros opcionais")
    fun findAll(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) status: VehicleStatus?
    ): List<VehicleResponse> = vehicleService.findAll(search, status)

    @GetMapping("/{id}")
    @Operation(summary = "Busca veículo por ID")
    fun findById(@PathVariable id: UUID): VehicleResponse =
        vehicleService.findById(id)

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados do veículo")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: VehicleUpdateRequest
    ): VehicleResponse = vehicleService.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove veículo do estoque")
    @PreAuthorize("hasAnyRole('ADMIN')")
    fun delete(@PathVariable id: UUID) =
        vehicleService.delete(id)

    @GetMapping("/summary")
    @Operation(summary = "Resumo do estoque por status")
    fun summary(): VehicleStockSummary =
        vehicleService.getStockSummary()
}
