package com.astro.leadflow.shared.modules.chatbot.session

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.UUID

class BotContextCacheTest {
    private val redisTemplate = mockk<StringRedisTemplate>(relaxed = true)
    private val ops = mockk<ValueOperations<String, String>>(relaxed = true)
    private val cache = BotContextCache(redisTemplate, ObjectMapper())

    @Test
    fun `get returns deserialized context`() {
        val sessionId = UUID.randomUUID()
        every { redisTemplate.opsForValue() } returns ops
        every { ops.get("leadflow:bot-session:$sessionId") } returns """{"intent":"greeting"}"""

        assertEquals(mapOf("intent" to "greeting"), cache.get(sessionId))
    }

    @Test
    fun `put stores json with ttl`() {
        val sessionId = UUID.randomUUID()
        every { redisTemplate.opsForValue() } returns ops

        cache.put(sessionId, mapOf("x" to "y"), Duration.ofMinutes(1))

        verify { ops.set("leadflow:bot-session:$sessionId", """{"x":"y"}""", Duration.ofMinutes(1)) }
    }
}
