package com.astro.leadflow.shared.modules.onboarding.api

import com.astro.leadflow.shared.modules.onboarding.api.requests.SubmitOnboardingRequest
import com.astro.leadflow.shared.modules.onboarding.service.OnboardingApplicationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/onboarding")
@Tag(name = "Onboarding — Público", description = "Solicitação pública de cadastro de nova concessionária")
class OnboardingController(
    private val onboardingService: OnboardingApplicationService
) {

    @PostMapping
    @Operation(summary = "Submete solicitação de onboarding de nova concessionária")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Solicitação recebida com sucesso"),
        ApiResponse(responseCode = "400", description = "Dados inválidos"),
        ApiResponse(responseCode = "409", description = "E-mail ou CNPJ já cadastrado")
    )
    fun submit(@Valid @RequestBody request: SubmitOnboardingRequest): ResponseEntity<Map<String, UUID>> {
        val id = onboardingService.submit(request)
        return ResponseEntity.status(201).body(mapOf("id" to id))
    }
}
