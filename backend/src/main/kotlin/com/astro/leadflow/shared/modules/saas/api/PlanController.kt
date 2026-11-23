package com.astro.leadflow.shared.modules.saas.api

import com.astro.leadflow.shared.modules.saas.api.responses.PlanResponse
import com.astro.leadflow.shared.modules.saas.domain.Plan
import com.astro.leadflow.shared.modules.saas.domain.PlanRepository
import com.astro.leadflow.shared.modules.saas.mapper.SaasMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "SaaS — Planos", description = "Planos disponíveis na plataforma")
class PlanController(
    private val planRepository: PlanRepository,
    private val saasMapper: SaasMapper
) {

    @GetMapping
    @Operation(
        summary = "Lista todos os planos disponíveis",
        description = "Endpoint público — usado na landing page e tela de upgrade"
    )
    fun listPlans(): List<PlanResponse> =
        planRepository.findAll()
            .sortedWith(compareBy<Plan>({ it.sortOrder }, { it.billingCycle.sortOrder }, { it.priceBrl }))
            .map { saasMapper.toPlanResponse(it) }
}
