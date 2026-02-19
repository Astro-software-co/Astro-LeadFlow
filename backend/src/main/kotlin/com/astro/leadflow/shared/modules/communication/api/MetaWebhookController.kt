package com.astro.leadflow.modules.communication.api

import com.astro.leadflow.modules.communication.api.requests.MetaWebhookRequest
import com.astro.leadflow.modules.communication.service.BotService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/webhooks/meta")
@Tag(name = "Communication — Webhook", description = "Webhook oficial da Meta/WhatsApp")
class MetaWebhookController(
    private val botService: BotService,
    @Value("\${meta.webhook-verify-token}") private val verifyToken: String
) {

    /**
     * Validação do webhook exigida pela Meta no cadastro.
     */
    @GetMapping
    fun verify(
        @RequestParam("hub.mode") mode: String,
        @RequestParam("hub.verify_token") token: String,
        @RequestParam("hub.challenge") challenge: String
    ): ResponseEntity<String> {
        if (mode == "subscribe" && token == verifyToken) {
            return ResponseEntity.ok(challenge)
        }
        return ResponseEntity.status(403).build()
    }

    /**
     * Recebe eventos da Meta: mensagens recebidas e atualizações de status.
     */
    @PostMapping
    fun receive(@RequestBody payload: MetaWebhookRequest): ResponseEntity<String> {
        payload.entry.forEach { entry ->
            entry.changes.forEach { change ->
                val value = change.value

                // Processa mensagens recebidas
                value.messages?.forEach { message ->
                    val contactName = value.contacts
                        ?.firstOrNull { it.wa_id == message.from }
                        ?.profile?.name
                    botService.handleIncoming(
                        phoneNumberId = value.metadata.phoneNumberId,
                        message = message,
                        contactName = contactName
                    )
                }

                // Processa atualizações de status (delivered, read, failed)
                value.statuses?.forEach { status ->
                    botService.handleStatusUpdate(status.id, status.status)
                }
            }
        }

        // Meta exige sempre HTTP 200 — caso contrário retenta o envio
        return ResponseEntity.ok("EVENT_RECEIVED")
    }
}