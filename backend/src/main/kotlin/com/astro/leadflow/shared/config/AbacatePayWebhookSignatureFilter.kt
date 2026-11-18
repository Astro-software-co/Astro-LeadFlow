package com.astro.leadflow.shared.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import java.security.MessageDigest
import java.time.Duration
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 11)
class AbacatePayWebhookSignatureFilter(
    @Value("\${app.abacatepay.webhook-secret:}") private val webhookSecret: String,
    @Value("\${app.abacatepay.webhook-public-key:}") private val webhookPublicKey: String,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val environment: Environment? = null,
    private val meterRegistry: MeterRegistry? = null,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val WEBHOOK_PATH = "/api/v1/webhooks/abacatepay"
        private const val SECRET_QUERY_PARAM = "webhookSecret"
        private const val SIGNATURE_HEADER = "X-Webhook-Signature"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val REPLAY_KEY_PREFIX = "leadflow:webhook:abacatepay:replay:"
        private val REPLAY_WINDOW = Duration.ofMinutes(10)
        private val PRODUCTION_PROFILES = setOf("prod", "production")
    }

    @PostConstruct
    fun validateConfiguration() {
        if (isProductionProfile() && (webhookSecret.isBlank() || webhookPublicKey.isBlank())) {
            throw IllegalStateException("app.abacatepay.webhook-secret e webhook-public-key devem estar configurados em produção")
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI != WEBHOOK_PATH || request.method != "POST"

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (webhookSecret.isBlank() || webhookPublicKey.isBlank()) {
            if (isProductionProfile()) {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "AbacatePay webhook is not configured")
                return
            }

            log.warn("Configuração do webhook AbacatePay ausente em ambiente não produtivo; validação ignorada")
            meterRegistry?.counter("leadflow.webhook.abacatepay.accepted", "mode", "no-secret-dev")?.increment()
            filterChain.doFilter(request, response)
            return
        }

        val providedSecret = request.getParameter(SECRET_QUERY_PARAM)
        if (providedSecret.isNullOrBlank() || providedSecret != webhookSecret) {
            meterRegistry?.counter("leadflow.webhook.abacatepay.rejected", "reason", "invalid-secret")?.increment()
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid webhook secret")
            return
        }

        val signatureHeader = request.getHeader(SIGNATURE_HEADER)
        if (signatureHeader.isNullOrBlank()) {
            meterRegistry?.counter("leadflow.webhook.abacatepay.rejected", "reason", "missing-signature")?.increment()
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing signature")
            return
        }

        val wrappedRequest = ContentCachingRequestWrapper(request)
        wrappedRequest.inputStream.readAllBytes()
        val body = wrappedRequest.contentAsByteArray

        val expectedSignature = computeBase64Hmac(webhookPublicKey, body)
        if (!MessageDigest.isEqual(expectedSignature.toByteArray(), signatureHeader.toByteArray())) {
            meterRegistry?.counter("leadflow.webhook.abacatepay.rejected", "reason", "invalid-signature")?.increment()
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid signature")
            return
        }

        val replayIdentifier = extractEventId(body) ?: signatureHeader
        val replayKey = REPLAY_KEY_PREFIX + replayIdentifier
        val accepted = redisTemplate.opsForValue().setIfAbsent(replayKey, "1", REPLAY_WINDOW)
        if (accepted != true) {
            meterRegistry?.counter("leadflow.webhook.abacatepay.rejected", "reason", "replay")?.increment()
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Replay detected")
            return
        }

        meterRegistry?.counter("leadflow.webhook.abacatepay.accepted", "mode", "signed")?.increment()
        filterChain.doFilter(wrappedRequest, response)
    }

    private fun computeBase64Hmac(secret: String, data: ByteArray): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(secret.toByteArray(), HMAC_ALGORITHM))
        return Base64.getEncoder().encodeToString(mac.doFinal(data))
    }

    private fun extractEventId(body: ByteArray): String? =
        runCatching {
            objectMapper.readTree(body).path("id").asText(null)?.takeIf { it.isNotBlank() }
        }.getOrNull()

    private fun isProductionProfile(): Boolean {
        val activeProfiles = environment?.activeProfiles ?: return false
        return activeProfiles.any { PRODUCTION_PROFILES.contains(it.lowercase()) }
    }
}
