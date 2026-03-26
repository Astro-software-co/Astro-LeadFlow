package com.astro.leadflow.shared.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Sliding window rate limiter using Redis sorted sets.
 *
 * Each request adds a timestamped entry to a sorted set keyed by the rate limit bucket.
 * Old entries (outside the window) are removed before counting. If the count exceeds
 * the configured limit, the request is rejected with 429 + Retry-After header.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
class RateLimitFilter(
    private val redisTemplate: RedisTemplate<String, Any>,
    @Value("\${app.rate-limit.webhook-per-minute:100}") private val webhookLimit: Int,
    @Value("\${app.rate-limit.auth-per-minute:10}") private val authLimit: Int,
    @Value("\${app.rate-limit.general-per-minute:500}") private val generalLimit: Int,
    @Value("\${app.rate-limit.enabled:true}") private val enabled: Boolean
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val WINDOW_SECONDS = 60L
        private const val PREFIX = "rate_limit:"
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        if (!enabled) return true
        val path = request.requestURI
        return path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator/health")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val bucket = resolveBucket(request)
        if (bucket == null) {
            filterChain.doFilter(request, response)
            return
        }

        val limit = bucket.second
        val key = bucket.first

        try {
            if (isRateLimited(key, limit)) {
                log.warn("Rate limit excedido. key={} limit={}/min", key, limit)
                response.setIntHeader("Retry-After", WINDOW_SECONDS.toInt())
                response.sendError(429, "Too many requests")
                return
            }
        } catch (e: Exception) {
            // Redis indisponível — fail open para não bloquear tráfego legítimo
            log.error("Erro ao verificar rate limit (fail open). key={} error={}", key, e.message)
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveBucket(request: HttpServletRequest): Pair<String, Int>? {
        val path = request.requestURI

        // POST /api/v1/webhooks/* — 100 req/min per API Key (or IP if no key)
        if (path.startsWith("/api/v1/webhooks/") && request.method == "POST") {
            val apiKey = request.getHeader("X-Api-Key")
            val identifier = apiKey ?: request.remoteAddr
            return "${PREFIX}webhook:$identifier" to webhookLimit
        }

        // POST /api/v1/auth/** — 10 req/min per IP
        if (path.startsWith("/api/v1/auth/")) {
            return "${PREFIX}auth:${request.remoteAddr}" to authLimit
        }

        // /api/v1/** geral — 500 req/min per organization (from JWT or API Key)
        if (path.startsWith("/api/v1/")) {
            val orgId = request.getAttribute(ApiKeyAuthFilter.ORG_ID_ATTRIBUTE) as? UUID
            val identifier = orgId?.toString() ?: request.remoteAddr
            return "${PREFIX}general:$identifier" to generalLimit
        }

        return null
    }

    private fun isRateLimited(key: String, limit: Int): Boolean {
        val now = Instant.now().toEpochMilli().toDouble()
        val windowStart = now - (WINDOW_SECONDS * 1000)

        val ops = redisTemplate.opsForZSet()
        ops.removeRangeByScore(key, Double.NEGATIVE_INFINITY, windowStart)
        val count = ops.zCard(key) ?: 0L

        if (count >= limit) {
            return true
        }

        ops.add(key, "$now:${UUID.randomUUID()}", now)
        redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS + 1))
        return false
    }
}
