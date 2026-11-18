package com.astro.leadflow.shared.config

import io.mockk.*
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Duration

class RateLimitFilterTest {

    private lateinit var redisTemplate: RedisTemplate<String, Any>
    private lateinit var zSetOps: ZSetOperations<String, Any>
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setup() {
        redisTemplate = mockk(relaxed = true)
        zSetOps = mockk(relaxed = true)
        filterChain = mockk(relaxed = true)

        every { redisTemplate.opsForZSet() } returns zSetOps
        every { redisTemplate.expire(any<String>(), any<Duration>()) } returns true
    }

    private fun filter(webhookLimit: Int = 100, authLimit: Int = 10, generalLimit: Int = 500) =
        RateLimitFilter(redisTemplate, webhookLimit, authLimit, generalLimit, true)

    @Test
    fun `should allow request when under limit`() {
        every { zSetOps.zCard(any()) } returns 5L
        every { zSetOps.add(any(), any(), any()) } returns true

        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/scraper")
        request.remoteAddr = "127.0.0.1"
        val response = MockHttpServletResponse()

        filter().doFilter(request, response, filterChain)

        assertEquals(200, response.status)
        verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should return 429 when webhook limit exceeded`() {
        every { zSetOps.zCard(any()) } returns 100L

        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/scraper")
        request.remoteAddr = "127.0.0.1"
        val response = MockHttpServletResponse()

        filter(webhookLimit = 100).doFilter(request, response, filterChain)

        assertEquals(429, response.status)
        assertEquals("60", response.getHeader("Retry-After"))
        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should return 429 when auth limit exceeded`() {
        every { zSetOps.zCard(any()) } returns 10L

        val request = MockHttpServletRequest("POST", "/api/v1/auth/login")
        request.remoteAddr = "192.168.1.1"
        val response = MockHttpServletResponse()

        filter(authLimit = 10).doFilter(request, response, filterChain)

        assertEquals(429, response.status)
        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should use API Key as identifier for webhook endpoints`() {
        every { zSetOps.zCard(any()) } returns 0L
        every { zSetOps.add(any(), any(), any()) } returns true

        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/scraper")
        request.addHeader("X-Api-Key", "my-api-key")
        val response = MockHttpServletResponse()

        filter().doFilter(request, response, filterChain)

        verify { zSetOps.removeRangeByScore(match { it.contains("webhook:my-api-key") }, any(), any()) }
    }

    @Test
    fun `should use IP as identifier for auth endpoints`() {
        every { zSetOps.zCard(any()) } returns 0L
        every { zSetOps.add(any(), any(), any()) } returns true

        val request = MockHttpServletRequest("POST", "/api/v1/auth/login")
        request.remoteAddr = "10.0.0.1"
        val response = MockHttpServletResponse()

        filter().doFilter(request, response, filterChain)

        verify { zSetOps.removeRangeByScore(match { it.contains("auth:10.0.0.1") }, any(), any()) }
    }

    @Test
    fun `should fail open when Redis is unavailable`() {
        every { zSetOps.removeRangeByScore(any(), any(), any()) } throws RuntimeException("Redis down")

        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/scraper")
        request.remoteAddr = "127.0.0.1"
        val response = MockHttpServletResponse()

        filter().doFilter(request, response, filterChain)

        assertEquals(200, response.status)
        verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should not filter swagger paths`() {
        val request = MockHttpServletRequest("GET", "/swagger-ui/index.html")
        val response = MockHttpServletResponse()

        filter().doFilter(request, response, filterChain)

        assertEquals(200, response.status)
        verify(exactly = 0) { zSetOps.zCard(any()) }
        verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should simulate burst exceeding limit`() {
        val filter = filter(webhookLimit = 3)
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/scraper")
        request.remoteAddr = "127.0.0.1"

        // First 3 requests are under limit
        every { zSetOps.zCard(any()) } returnsMany listOf(0L, 1L, 2L, 3L)
        every { zSetOps.add(any(), any(), any()) } returns true

        repeat(3) {
            val resp = MockHttpServletResponse()
            filter.doFilter(request, resp, filterChain)
            assertEquals(200, resp.status)
        }

        // 4th request exceeds limit
        val resp = MockHttpServletResponse()
        filter.doFilter(request, resp, filterChain)
        assertEquals(429, resp.status)
    }
}
