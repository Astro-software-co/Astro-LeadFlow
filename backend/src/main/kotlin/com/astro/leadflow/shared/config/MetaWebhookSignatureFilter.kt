package com.astro.leadflow.shared.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class MetaWebhookSignatureFilter(
    @Value("\${app.meta.webhook-secret:}") private val webhookSecret: String
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val META_WEBHOOK_PATH = "/api/v1/webhooks/meta"
        private const val SIGNATURE_HEADER = "X-Hub-Signature-256"
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.requestURI != META_WEBHOOK_PATH || request.method != "POST"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (webhookSecret.isBlank()) {
            log.warn("app.meta.webhook-secret não configurado — assinatura Meta não validada")
            filterChain.doFilter(request, response)
            return
        }

        val signatureHeader = request.getHeader(SIGNATURE_HEADER)
        if (signatureHeader.isNullOrBlank()) {
            log.warn("Requisição Meta webhook sem header {}. ip={}", SIGNATURE_HEADER, request.remoteAddr)
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing signature")
            return
        }

        val wrappedRequest = ContentCachingRequestWrapper(request)
        // Force reading the body so it's cached
        wrappedRequest.inputStream.readAllBytes()
        val body = wrappedRequest.contentAsByteArray

        val expectedSignature = computeHmacSha256(webhookSecret, body)
        val receivedSignature = signatureHeader.removePrefix("sha256=")

        if (!MessageDigest.isEqual(expectedSignature.toByteArray(), receivedSignature.toByteArray())) {
            log.warn("Assinatura Meta webhook inválida. ip={}", request.remoteAddr)
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid signature")
            return
        }

        filterChain.doFilter(wrappedRequest, response)
    }

    private fun computeHmacSha256(secret: String, data: ByteArray): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(secret.toByteArray(), HMAC_ALGORITHM))
        return mac.doFinal(data).joinToString("") { "%02x".format(it) }
    }
}
