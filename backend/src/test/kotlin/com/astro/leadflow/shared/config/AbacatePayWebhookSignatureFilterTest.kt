package com.astro.leadflow.shared.config

import com.fasterxml.jackson.databind.ObjectMapper
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
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AbacatePayWebhookSignatureFilterTest {

    private val secret = "abacate-secret"
    private val publicKey = "public-key-example"
    private val redisTemplate: RedisTemplate<String, Any> = mockk()
    private val valueOperations: ValueOperations<String, Any> = mockk()
    private val filterChain: FilterChain = mockk(relaxed = true)
    private val objectMapper = ObjectMapper()

    private fun newFilter(
        secret: String = this.secret,
        publicKey: String = this.publicKey,
        environment: MockEnvironment? = null,
    ): AbacatePayWebhookSignatureFilter {
        every { redisTemplate.opsForValue() } returns valueOperations
        return AbacatePayWebhookSignatureFilter(secret, publicKey, redisTemplate, objectMapper, environment)
    }

    private fun computeSignature(body: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(publicKey.toByteArray(), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(body.toByteArray()))
    }

    @Test
    fun `should reject when webhook secret is missing or invalid`() {
        val filter = newFilter()
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/abacatepay")
        request.setContent("""{"id":"evt_1"}""".toByteArray())
        request.addHeader("X-Webhook-Signature", computeSignature("""{"id":"evt_1"}"""))
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertEquals(403, response.status)
        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should reject when signature is invalid`() {
        val filter = newFilter()
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/abacatepay")
        request.queryString = "webhookSecret=$secret"
        request.setParameter("webhookSecret", secret)
        request.setContent("""{"id":"evt_2"}""".toByteArray())
        request.addHeader("X-Webhook-Signature", "invalid")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertEquals(403, response.status)
        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should accept first valid event`() {
        val filter = newFilter()
        val body = """{"id":"evt_3","event":"subscription.completed"}"""
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/abacatepay")
        request.setParameter("webhookSecret", secret)
        request.setContent(body.toByteArray())
        request.addHeader("X-Webhook-Signature", computeSignature(body))
        val response = MockHttpServletResponse()

        every {
            valueOperations.setIfAbsent(
                "leadflow:webhook:abacatepay:replay:evt_3",
                "1",
                Duration.ofMinutes(10),
            )
        } returns true

        filter.doFilter(request, response, filterChain)

        assertEquals(200, response.status)
        verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should reject replayed event`() {
        val filter = newFilter()
        val body = """{"id":"evt_4","event":"subscription.completed"}"""
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/abacatepay")
        request.setParameter("webhookSecret", secret)
        request.setContent(body.toByteArray())
        request.addHeader("X-Webhook-Signature", computeSignature(body))
        val response = MockHttpServletResponse()

        every {
            valueOperations.setIfAbsent(
                "leadflow:webhook:abacatepay:replay:evt_4",
                "1",
                Duration.ofMinutes(10),
            )
        } returns false

        filter.doFilter(request, response, filterChain)

        assertEquals(403, response.status)
        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should fail fast on production profile when config is blank`() {
        val environment = MockEnvironment().also { it.setActiveProfiles("prod") }

        val exception = assertThrows<IllegalStateException> {
            AbacatePayWebhookSignatureFilter("", "", redisTemplate, objectMapper, environment).validateConfiguration()
        }

        assertEquals(
            "app.abacatepay.webhook-secret e webhook-public-key devem estar configurados em produção",
            exception.message,
        )
    }
}
