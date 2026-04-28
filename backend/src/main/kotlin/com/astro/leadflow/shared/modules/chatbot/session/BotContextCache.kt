package com.astro.leadflow.shared.modules.chatbot.session

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class BotContextCache(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    fun get(sessionId: UUID): Map<String, String> {
        val payload = redisTemplate.opsForValue().get(key(sessionId)) ?: return emptyMap()
        return objectMapper.readValue(payload, object : TypeReference<Map<String, String>>() {})
    }

    fun put(sessionId: UUID, context: Map<String, String>, ttl: Duration = DEFAULT_TTL) {
        redisTemplate.opsForValue().set(key(sessionId), objectMapper.writeValueAsString(context), ttl)
    }

    fun evict(sessionId: UUID) {
        redisTemplate.delete(key(sessionId))
    }

    private fun key(sessionId: UUID): String = "leadflow:bot-session:$sessionId"

    companion object {
        val DEFAULT_TTL: Duration = Duration.ofMinutes(30)
    }
}
