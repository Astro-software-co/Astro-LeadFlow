package com.astro.leadflow.shared.modules.iam.api

import com.astro.leadflow.shared.modules.iam.api.requests.UpdateOrganizationRequest
import com.astro.leadflow.shared.modules.iam.api.requests.UpdateWhatsappCredentialsRequest
import com.astro.leadflow.shared.modules.iam.api.responses.OrganizationResponse
import com.astro.leadflow.shared.modules.iam.service.OrganizationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/organization")
@Tag(name = "IAM — Organização", description = "Dados e configurações da organização")
class OrganizationController(
    private val organizationService: OrganizationService
) {

    @GetMapping
    @Operation(summary = "Retorna dados da organização do usuário logado")
    @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso")
    fun getMyOrganization(): OrganizationResponse =
        organizationService.getMyOrganization()

    @PutMapping
    @Operation(summary = "Atualiza dados da organização")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Organização atualizada"),
        ApiResponse(responseCode = "400", description = "Dados inválidos"),
        ApiResponse(responseCode = "403", description = "Apenas ADMIN pode alterar a organização")
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun update(@Valid @RequestBody request: UpdateOrganizationRequest): OrganizationResponse =
        organizationService.update(request)

    @PatchMapping("/whatsapp")
    @Operation(
        summary = "Atualiza credenciais do WhatsApp Business",
        description = "Atualiza token e phone number ID da integração com a Meta"
    )
    @ApiResponse(responseCode = "403", description = "Apenas ADMIN pode alterar credenciais")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateWhatsapp(
        @Valid @RequestBody request: UpdateWhatsappCredentialsRequest
    ): OrganizationResponse =
        organizationService.updateWhatsappCredentials(request)
}