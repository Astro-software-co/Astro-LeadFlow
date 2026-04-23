package com.astro.leadflow.shared.modules.saas.api

import com.astro.leadflow.shared.modules.saas.api.requests.AbacatePayWebhookEvent
import com.astro.leadflow.shared.modules.saas.service.AbacatePaySubscriptionService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/webhooks/abacatepay")
@Tag(name = "SaaS — Webhook AbacatePay", description = "Recebe eventos de cobrança e assinatura")
class AbacatePayWebhookController(
    private val abacatePaySubscriptionService: AbacatePaySubscriptionService,
) {

    @PostMapping
    fun receive(@RequestBody event: AbacatePayWebhookEvent): ResponseEntity<String> {
        abacatePaySubscriptionService.handleWebhook(event)
        return ResponseEntity.ok("OK")
    }
}
