package com.astro.leadflow.shared.modules.onboarding.api

import com.astro.leadflow.shared.modules.onboarding.api.requests.RejectOnboardingRequest
import com.astro.leadflow.shared.modules.onboarding.api.responses.ApproveOnboardingResponse
import com.astro.leadflow.shared.modules.onboarding.api.responses.OnboardingApplicationDetailResponse
import com.astro.leadflow.shared.modules.onboarding.api.responses.OnboardingApplicationSummaryResponse
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingStatus
import com.astro.leadflow.shared.modules.onboarding.service.OnboardingApplicationService
import com.astro.leadflow.shared.modules.onboarding.service.OnboardingApprovalService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping(value = ["/api/v1/admin/onboarding", "/api/admin/onboarding"])
@PreAuthorize("hasRole('ASTRO_ADMIN')")
@Tag(name = "Admin — Onboarding", description = "Gestão de solicitações de onboarding de concessionárias (Astro Admin)")
class AdminOnboardingController(
    private val onboardingService: OnboardingApplicationService,
    private val approvalService: OnboardingApprovalService
) {

    @GetMapping
    @Operation(summary = "Lista solicitações de onboarding com filtros opcionais")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        ApiResponse(responseCode = "403", description = "Acesso negado — requer ASTRO_ADMIN")
    )
    fun list(
        @RequestParam(required = false) status: OnboardingStatus?,
        @RequestParam(required = false) query: String?,
        @PageableDefault(size = 20, sort = ["createdAt"]) pageable: Pageable
    ): Page<OnboardingApplicationSummaryResponse> = onboardingService.list(status, query, pageable)

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma solicitação de onboarding por ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Detalhe retornado com sucesso"),
        ApiResponse(responseCode = "404", description = "Solicitação não encontrada"),
        ApiResponse(responseCode = "403", description = "Acesso negado — requer ASTRO_ADMIN")
    )
    fun getById(@PathVariable id: UUID): OnboardingApplicationDetailResponse = onboardingService.getById(id)

    @PostMapping("/{id}/approve")
    @Operation(summary = "Aprova uma solicitação de onboarding e cria organização + usuário admin")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Solicitação aprovada com sucesso"),
        ApiResponse(responseCode = "400", description = "Solicitação não está pendente"),
        ApiResponse(responseCode = "404", description = "Solicitação não encontrada"),
        ApiResponse(responseCode = "403", description = "Acesso negado — requer ASTRO_ADMIN")
    )
    fun approve(@PathVariable id: UUID): ApproveOnboardingResponse = approvalService.approve(id)

    @PostMapping("/{id}/reject")
    @Operation(summary = "Rejeita uma solicitação de onboarding com motivo")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Solicitação rejeitada com sucesso"),
        ApiResponse(responseCode = "400", description = "Motivo de rejeição inválido ou solicitação não pendente"),
        ApiResponse(responseCode = "404", description = "Solicitação não encontrada"),
        ApiResponse(responseCode = "403", description = "Acesso negado — requer ASTRO_ADMIN")
    )
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    fun reject(@PathVariable id: UUID, @Valid @RequestBody request: RejectOnboardingRequest) {
        approvalService.reject(id, request.rejectionReason)
    }
}
