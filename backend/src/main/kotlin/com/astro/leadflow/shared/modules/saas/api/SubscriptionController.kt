package com.astro.leadflow.modules.saas.api

import com.astro.leadflow.modules.saas.service.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/saas")
@Tag(name = "SaaS — Assinatura", description = "Planos, quotas e marketplaces")
@PreAuthorize("hasAnyRole('ADMIN')")
class SubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val usageLimitService: UsageLimitService
) {

    // ── Assinatura ───────────────────────────────────────────

    @GetMapping("/subscription")
    @Operation(summary = "Retorna assinatura atual da organização")
    fun getSubscription(): SubscriptionResponse =
        subscriptionService.getMySubscription()

    @PatchMapping("/subscription/plan/{planId}")
    @Operation(summary = "Altera plano da organização")
    fun changePlan(@PathVariable planId: UUID): SubscriptionResponse =
        subscriptionService.changePlan(planId)

    // ── Uso de mensagens ─────────────────────────────────────

    @GetMapping("/usage")
    @Operation(summary = "Resumo de uso de mensagens do mês atual")
    fun getUsage(): UsageSummaryResponse {
        val orgId = com.astro.leadflow.shared.util.SecurityUtils.getCurrentOrganizationId()
        return usageLimitService.getUsageSummary(orgId)
    }

    // ── Marketplaces ─────────────────────────────────────────

    @GetMapping("/marketplaces")
    @Operation(summary = "Lista marketplaces configurados")
    fun listMarketplaces(): List<MarketplaceConfigResponse> =
        subscriptionService.listMarketplaces()

    @PostMapping("/marketplaces")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona novo marketplace")
    fun addMarketplace(@Valid @RequestBody request: AddMarketplaceRequest): MarketplaceConfigResponse =
        subscriptionService.addMarketplace(request.marketplaceName, request.monthlyLimit)

    @PatchMapping("/marketplaces/{id}")
    @Operation(summary = "Atualiza configuração de marketplace")
    fun updateMarketplace(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateMarketplaceRequest
    ): MarketplaceConfigResponse =
        subscriptionService.updateMarketplace(id, request.enabled, request.monthlyLimit)

    @DeleteMapping("/marketplaces/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove marketplace")
    fun removeMarketplace(@PathVariable id: UUID) =
        subscriptionService.removeMarketplace(id)
}

// ─── Request DTOs ────────────────────────────────────────────

data class AddMarketplaceRequest(
    @field:NotBlank val marketplaceName: String,
    @field:NotNull @field:Min(1) val monthlyLimit: Int
)

data class UpdateMarketplaceRequest(
    val enabled: Boolean? = null,
    @field:Min(1) val monthlyLimit: Int? = null
)