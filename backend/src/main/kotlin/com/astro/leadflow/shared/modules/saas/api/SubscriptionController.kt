package com.astro.leadflow.shared.modules.saas.api

import com.astro.leadflow.shared.modules.saas.api.requests.AddMarketplaceRequest
import com.astro.leadflow.shared.modules.saas.api.requests.CreateSubscriptionCheckoutRequest
import com.astro.leadflow.shared.modules.saas.api.requests.UpdateMarketplaceRequest
import com.astro.leadflow.shared.modules.saas.api.responses.BillingHistoryResponse
import com.astro.leadflow.shared.modules.saas.api.responses.MarketplaceConfigResponse
import com.astro.leadflow.shared.modules.saas.api.responses.SubscriptionCheckoutResponse
import com.astro.leadflow.shared.modules.saas.api.responses.SubscriptionResponse
import com.astro.leadflow.shared.modules.saas.api.responses.UsageSummaryResponse
import com.astro.leadflow.shared.modules.saas.service.AbacatePaySubscriptionService
import com.astro.leadflow.shared.modules.saas.service.SubscriptionService
import com.astro.leadflow.shared.modules.saas.service.UsageLimitService
import com.astro.leadflow.shared.util.SecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/saas")
@Tag(name = "SaaS — Assinatura", description = "Planos, quotas e marketplaces")
@PreAuthorize("hasRole('ADMIN')")
class SubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val usageLimitService: UsageLimitService,
    private val abacatePaySubscriptionService: AbacatePaySubscriptionService,
) {

    @GetMapping("/subscription")
    @Operation(summary = "Retorna assinatura atual da organização")
    @ApiResponse(responseCode = "200", description = "Assinatura retornada com sucesso")
    fun getSubscription(): SubscriptionResponse =
        subscriptionService.getMySubscription()

    @PatchMapping("/subscription/plan/{planId}")
    @Operation(summary = "Altera plano da organização")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Plano alterado com sucesso"),
        ApiResponse(responseCode = "404", description = "Plano não encontrado")
    )
    fun changePlan(@PathVariable planId: UUID): SubscriptionResponse =
        subscriptionService.changePlan(planId)

    @PostMapping("/subscription/checkout/{planId}")
    @Operation(summary = "Cria checkout self-service da assinatura via AbacatePay")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Checkout criado com sucesso"),
        ApiResponse(responseCode = "404", description = "Plano não encontrado"),
        ApiResponse(responseCode = "422", description = "Plano indisponível para self-service")
    )
    fun createSubscriptionCheckout(
        @PathVariable planId: UUID,
        @Valid @RequestBody(required = false) request: CreateSubscriptionCheckoutRequest?,
    ): SubscriptionCheckoutResponse =
        abacatePaySubscriptionService.createCheckout(
            planId = planId,
            request = request ?: CreateSubscriptionCheckoutRequest(),
        )

    @PostMapping("/subscription/cancel")
    @Operation(summary = "Cancela assinatura ativa via AbacatePay")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Assinatura cancelada com sucesso"),
        ApiResponse(responseCode = "400", description = "Não há assinatura externa ativa para cancelar")
    )
    fun cancelSubscription(): SubscriptionResponse =
        abacatePaySubscriptionService.cancelCurrentSubscription()

    @GetMapping("/usage")
    @Operation(summary = "Resumo de uso de mensagens do mês atual")
    @ApiResponse(responseCode = "200", description = "Uso retornado com sucesso")
    fun getUsage(): UsageSummaryResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return usageLimitService.getUsageSummary(orgId)
    }

    @GetMapping("/billing-history")
    @Operation(summary = "Historico de cobrancas da organizacao")
    @ApiResponse(responseCode = "200", description = "Historico retornado com sucesso")
    fun getBillingHistory(): List<BillingHistoryResponse> =
        subscriptionService.listBillingHistory()

    @GetMapping("/marketplaces")
    @Operation(summary = "Lista marketplaces configurados")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    fun listMarketplaces(): List<MarketplaceConfigResponse> =
        subscriptionService.listMarketplaces()

    @PostMapping("/marketplaces")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona novo marketplace")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Marketplace adicionado com sucesso"),
        ApiResponse(responseCode = "400", description = "Dados inválidos"),
        ApiResponse(responseCode = "422", description = "Limite de marketplaces do plano atingido")
    )
    fun addMarketplace(
        @Valid @RequestBody request: AddMarketplaceRequest
    ): MarketplaceConfigResponse =
        subscriptionService.addMarketplace(request.marketplaceName, request.monthlyLimit)

    @PatchMapping("/marketplaces/{id}")
    @Operation(summary = "Atualiza configuração de marketplace")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Marketplace atualizado com sucesso"),
        ApiResponse(responseCode = "404", description = "Marketplace não encontrado")
    )
    fun updateMarketplace(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateMarketplaceRequest
    ): MarketplaceConfigResponse =
        subscriptionService.updateMarketplace(id, request.enabled, request.monthlyLimit)

    @DeleteMapping("/marketplaces/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove marketplace")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Marketplace removido com sucesso"),
        ApiResponse(responseCode = "404", description = "Marketplace não encontrado")
    )
    fun removeMarketplace(@PathVariable id: UUID) =
        subscriptionService.removeMarketplace(id)
}
