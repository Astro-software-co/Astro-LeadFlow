package com.astro.leadflow.shared.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Duration
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class MetaWebhookSignatureFilterTest {

    private val secret = "test-webhook-secret"
    private val redisTemplate: RedisTemplate<String, Any> = mockk()
    private val valueOperations: ValueOperations<String, Any> = mockk()
    private val filterChain: FilterChain = mockk(relaxed = true)

    private fun newFilter(secret: String = this.secret): MetaWebhookSignatureFilter {
        every { redisTemplate.opsForValue() } returns valueOperations
        return MetaWebhookSignatureFilter(secret, redisTemplate)
    }

    private fun computeSignature(body: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return "sha256=" + mac.doFinal(body.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `should return 403 when signature header is missing`() {
        val filter = newFilter()
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/meta")
        request.setContent("""{"entry":[]}""".toByteArray())
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertEquals(403, response.status)
        verify(exactly = 0) { valueOperations.setIfAbsent(any(), any(), any<Duration>()) }
        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should return 403 when signature is invalid`() {
        val filter = newFilter()
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/meta")
        request.setContent("""{"entry":[]}""".toByteArray())
        request.addHeader("X-Hub-Signature-256", "sha256=invalid")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertEquals(403, response.status)
        verify(exactly = 0) { valueOperations.setIfAbsent(any(), any(), any<Duration>()) }
        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should accept first delivery id event`() {
        val filter = newFilter()
        val body = """{"entry":[]}"""
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/meta")
        request.setContent(body.toByteArray())
        request.addHeader("X-Hub-Signature-256", computeSignature(body))
        request.addHeader("X-Hub-Delivery", "evt-123")
        val response = MockHttpServletResponse()

        every {
            valueOperations.setIfAbsent(
                "leadflow:webhook:meta:replay:delivery:evt-123",
                "1",
                Duration.ofMinutes(5),
            )
        } returns true

        filter.doFilter(request, response, filterChain)

        assertEquals(200, response.status)
        verify(exactly = 1) {
            valueOperations.setIfAbsent(
                "leadflow:webhook:meta:replay:delivery:evt-123",
                "1",
                Duration.ofMinutes(5),
            )
        }
        verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should reject replay by delivery id`() {
        val filter = newFilter()
        val body = """{"entry":[]}"""
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/meta")
        request.setContent(body.toByteArray())
        request.addHeader("X-Hub-Signature-256", computeSignature(body))
        request.addHeader("X-Hub-Delivery", "evt-123")
        val response = MockHttpServletResponse()

        every {
            valueOperations.setIfAbsent(
                "leadflow:webhook:meta:replay:delivery:evt-123",
                "1",
                Duration.ofMinutes(5),
            )
        } returns false

        filter.doFilter(request, response, filterChain)

        assertEquals(403, response.status)
        verify(exactly = 1) {
            valueOperations.setIfAbsent(
                "leadflow:webhook:meta:replay:delivery:evt-123",
                "1",
                Duration.ofMinutes(5),
            )
        }
        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should reject replay by signature fallback when delivery id is missing`() {
        val filter = newFilter()
        val body = """{"entry":[]}"""
        val signature = computeSignature(body).removePrefix("sha256=")
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/meta")
        request.setContent(body.toByteArray())
        request.addHeader("X-Hub-Signature-256", "sha256=" + signature)
        val response = MockHttpServletResponse()

        every {
            valueOperations.setIfAbsent(
                "leadflow:webhook:meta:replay:signature:" + signature,
                "1",
                Duration.ofMinutes(5),
            )
        } returns false

        filter.doFilter(request, response, filterChain)

        assertEquals(403, response.status)
        verify(exactly = 1) {
            valueOperations.setIfAbsent(
                "leadflow:webhook:meta:replay:signature:" + signature,
                "1",
                Duration.ofMinutes(5),
            )
        }
        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should fail fast on production profile when secret is blank`() {
        val prodEnvironment = MockEnvironment().also { it.setActiveProfiles("prod") }

        val exception = assertThrows<IllegalStateException> {
            MetaWebhookSignatureFilter("", redisTemplate, prodEnvironment).validateConfiguration()
        }

        assertEquals(
            "app.meta.webhook-secret must be configured when running in production profile",
            exception.message,
        )
    }
}
