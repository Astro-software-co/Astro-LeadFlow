package com.astro.leadflow.shared.modules.inventory.api

import com.astro.leadflow.shared.modules.inventory.api.responses.VehiclePhotoResponse
import com.astro.leadflow.shared.modules.inventory.service.VehiclePhotoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/photos")
@Tag(name = "Inventory — Fotos", description = "Upload e gestão de fotos de veículos")
@PreAuthorize("hasRole('MANAGER')")
class VehiclePhotoController(
    private val vehiclePhotoService: VehiclePhotoService
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Faz upload de foto para o veículo")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Foto enviada com sucesso"),
        ApiResponse(responseCode = "400", description = "Arquivo inválido"),
        ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    )
    fun upload(
        @PathVariable vehicleId: UUID,
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false, defaultValue = "0") position: Int
    ): VehiclePhotoResponse =
        vehiclePhotoService.upload(vehicleId, file, position)

    @GetMapping
    @Operation(summary = "Lista todas as fotos do veículo")
    fun listPhotos(@PathVariable vehicleId: UUID): List<VehiclePhotoResponse> =
        vehiclePhotoService.listPhotos(vehicleId)

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove foto do veículo")
    @ApiResponse(responseCode = "204", description = "Foto removida com sucesso")
    fun delete(
        @PathVariable vehicleId: UUID,
        @PathVariable photoId: UUID
    ) = vehiclePhotoService.delete(vehicleId, photoId)

    @PatchMapping("/reorder")
    @Operation(summary = "Reordena fotos do veículo")
    fun reorder(
        @PathVariable vehicleId: UUID,
        @Valid @RequestBody request: ReorderPhotosRequest
    ): List<VehiclePhotoResponse> =
        vehiclePhotoService.reorder(vehicleId, request.photoIds)
}

data class ReorderPhotosRequest(
    @field:NotEmpty val photoIds: List<UUID>
)
