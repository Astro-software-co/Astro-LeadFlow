package com.astro.leadflow.shared.modules.iam.service

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

@Service
class RefreshTokenBlocklistService(
    private val redisTemplate: RedisTemplate<String, Any>,
) {

    companion object {
        private const val PREFIX = "leadflow:auth:refresh:blocklist:"
    }

    fun revoke(refreshToken: String, expiresAt: Instant): Boolean {
        val ttl = Duration.between(Instant.now(), expiresAt)
            .let { if (it.isNegative || it.isZero) Duration.ofSeconds(1) else it }

        val key = buildKey(refreshToken)
        return redisTemplate.opsForValue().setIfAbsent(key, true, ttl) == true
    }

    fun isRevoked(refreshToken: String): Boolean =
        redisTemplate.hasKey(buildKey(refreshToken)) == true

    private fun buildKey(refreshToken: String): String =
        PREFIX + hash(refreshToken)

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
