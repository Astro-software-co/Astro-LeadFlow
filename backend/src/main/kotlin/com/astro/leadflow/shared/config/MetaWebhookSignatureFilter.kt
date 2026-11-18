package com.astro.leadflow.shared.config

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
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class MetaWebhookSignatureFilter(
    @Value("\${app.meta.webhook-secret:}") private val webhookSecret: String,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val environment: Environment? = null,
    private val meterRegistry: MeterRegistry? = null,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val META_WEBHOOK_PATH = "/api/v1/webhooks/meta"
        private const val SIGNATURE_HEADER = "X-Hub-Signature-256"
        private const val DELIVERY_ID_HEADER = "X-Hub-Delivery"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val REPLAY_KEY_PREFIX = "leadflow:webhook:meta:replay:"
        private val REPLAY_WINDOW = Duration.ofMinutes(5)
        private val PRODUCTION_PROFILES = setOf("prod", "production")
    }

    @PostConstruct
    fun validateConfiguration() {
        if (isProductionProfile() && webhookSecret.isBlank()) {
            throw IllegalStateException("app.meta.webhook-secret must be configured when running in production profile")
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.requestURI != META_WEBHOOK_PATH || request.method != "POST"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (webhookSecret.isBlank()) {
            if (isProductionProfile()) {
                log.error("app.meta.webhook-secret ausente em profile de producao")
                meterRegistry?.counter("leadflow.webhook.meta.rejected", "reason", "secret-missing")?.increment()
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Webhook secret is not configured")
                return
            }

            log.warn("app.meta.webhook-secret não configurado — assinatura Meta não validada")
            meterRegistry?.counter("leadflow.webhook.meta.accepted", "mode", "no-secret-dev")?.increment()
            filterChain.doFilter(request, response)
            return
        }

        val signatureHeader = request.getHeader(SIGNATURE_HEADER)
        if (signatureHeader.isNullOrBlank()) {
            log.warn("Requisição Meta webhook sem header {}. ip={}", SIGNATURE_HEADER, request.remoteAddr)
            meterRegistry?.counter("leadflow.webhook.meta.rejected", "reason", "missing-signature")?.increment()
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing signature")
            return
        }

        val wrappedRequest = ContentCachingRequestWrapper(request)
        wrappedRequest.inputStream.readAllBytes()
        val body = wrappedRequest.contentAsByteArray

        val expectedSignature = computeHmacSha256(webhookSecret, body)
        val receivedSignature = signatureHeader.removePrefix("sha256=")

        if (!MessageDigest.isEqual(expectedSignature.toByteArray(), receivedSignature.toByteArray())) {
            log.warn("Assinatura Meta webhook inválida. ip={}", request.remoteAddr)
            meterRegistry?.counter("leadflow.webhook.meta.rejected", "reason", "invalid-signature")?.increment()
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid signature")
            return
        }

        val deliveryId = request.getHeader(DELIVERY_ID_HEADER)?.trim()?.takeIf { it.isNotEmpty() }
        val replaySource = if (deliveryId != null) "delivery-id" else "signature-fallback"
        val replayKey = if (deliveryId != null) {
            REPLAY_KEY_PREFIX + "delivery:" + deliveryId
        } else {
            REPLAY_KEY_PREFIX + "signature:" + receivedSignature
        }

        if (isReplay(replayKey)) {
            log.warn("Replay detectado em webhook Meta. source={} ip={}", replaySource, request.remoteAddr)
            meterRegistry?.counter(
                "leadflow.webhook.meta.rejected",
                "reason",
                "replay",
                "source",
                replaySource,
            )?.increment()
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Replay detected")
            return
        }

        meterRegistry?.counter("leadflow.webhook.meta.accepted", "mode", "signed")?.increment()
        filterChain.doFilter(wrappedRequest, response)
    }

    private fun computeHmacSha256(secret: String, data: ByteArray): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(secret.toByteArray(), HMAC_ALGORITHM))
        return mac.doFinal(data).joinToString("") { "%02x".format(it) }
    }

    private fun isProductionProfile(): Boolean {
        val activeProfiles = environment?.activeProfiles ?: return false
        return activeProfiles.any { profile -> PRODUCTION_PROFILES.contains(profile.lowercase()) }
    }

    private fun isReplay(replayKey: String): Boolean {
        val accepted = redisTemplate.opsForValue().setIfAbsent(replayKey, "1", REPLAY_WINDOW)
        return accepted != true
    }
}
