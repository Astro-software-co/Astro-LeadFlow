package com.astro.leadflow.shared.modules.ingestion.api.controller

import com.astro.leadflow.shared.modules.ingestion.service.marketplace.MercadoLivreAuthService
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/v1/auth/mercadolivre")
@Tag(name = "Ingestion — Mercado Livre OAuth", description = "Fluxo OAuth2 para conexão com Mercado Livre")
class MercadoLivreAuthController(
    private val mlAuthService: MercadoLivreAuthService,
    private val organizationRepository: OrganizationRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/connect")
    @Operation(
        summary = "Inicia conexão OAuth2 com Mercado Livre",
        description = "Retorna a URL de autorização para redirecionar o lojista ao ML"
    )
    fun connect(@RequestParam organizationId: UUID): Map<String, String> {
        val state = "${organizationId}:${UUID.randomUUID()}"
        val url = mlAuthService.buildAuthorizationUrl(state)
        return mapOf("authorizationUrl" to url)
    }

    @GetMapping("/callback")
    @Operation(
        summary = "Callback OAuth2 do Mercado Livre",
        description = "Recebe o code e state após autorização do lojista. Troca pelo access_token."
    )
    fun callback(
        @RequestParam code: String,
        @RequestParam state: String
    ): Map<String, String> {
        // state = "{organizationId}:{nonce}"
        val organizationId = runCatching {
            UUID.fromString(state.substringBefore(":"))
        }.getOrElse {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "State inválido")
        }

        val organization = organizationRepository.findById(organizationId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Organização não encontrada")
        }

        val credential = mlAuthService.exchangeCodeForTokens(code, organization)
        log.info("Mercado Livre conectado com sucesso. org={} mlUserId={}", organizationId, credential.mlUserId)

        return mapOf(
            "status" to "connected",
            "mlUserId" to (credential.mlUserId?.toString() ?: "")
        )
    }
}
