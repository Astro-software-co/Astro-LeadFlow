package com.astro.leadflow.shared.config

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class MetaWebhookSignatureFilterTest {

    private val secret = "test-webhook-secret"
    private val filter = MetaWebhookSignatureFilter(secret)
    private val filterChain: FilterChain = mockk(relaxed = true)

    private fun computeSignature(body: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return "sha256=" + mac.doFinal(body.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `should return 403 when signature header is missing`() {
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/meta")
        request.setContent("""{"entry":[]}""".toByteArray())
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertEquals(403, response.status)
        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should return 403 when signature is invalid`() {
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/meta")
        request.setContent("""{"entry":[]}""".toByteArray())
        request.addHeader("X-Hub-Signature-256", "sha256=invalid")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertEquals(403, response.status)
        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should pass through when signature is valid`() {
        val body = """{"entry":[]}"""
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/meta")
        request.setContent(body.toByteArray())
        request.addHeader("X-Hub-Signature-256", computeSignature(body))
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertEquals(200, response.status)
        verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should not filter GET requests`() {
        val request = MockHttpServletRequest("GET", "/api/v1/webhooks/meta")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertEquals(200, response.status)
        verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should not filter non-meta paths`() {
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/webmotors")
        request.setContent("""{}""".toByteArray())
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertEquals(200, response.status)
        verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    }
}
