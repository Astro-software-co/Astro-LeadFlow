package com.astro.leadflow.shared.modules.iam.service

import com.astro.leadflow.shared.modules.iam.domain.Role
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class AuthSessionRecord(
    val sessionId: String,
    val userId: UUID,
    val organizationId: UUID?,
    val role: Role,
    val refreshTokenId: String,
    val createdAt: Instant,
    val lastActivityAt: Instant,
)

@Service
class AuthSessionService(
    private val redisTemplate: RedisTemplate<String, Any>,
    @Value("\${app.auth.session.idle-timeout-ms:900000}")
    private val idleTimeoutMs: Long,
) {

    companion object {
        private const val PREFIX = "leadflow:auth:session:"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_ORGANIZATION_ID = "organizationId"
        private const val FIELD_ROLE = "role"
        private const val FIELD_REFRESH_TOKEN_ID = "refreshTokenId"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_LAST_ACTIVITY_AT = "lastActivityAt"
    }

    fun create(
        sessionId: String,
        userId: UUID,
        organizationId: UUID?,
        role: Role,
        refreshTokenId: String,
    ): AuthSessionRecord {
        val now = Instant.now()
        val key = buildKey(sessionId)
        val payload = mapOf(
            FIELD_USER_ID to userId.toString(),
            FIELD_ORGANIZATION_ID to organizationId?.toString().orEmpty(),
            FIELD_ROLE to role.name,
            FIELD_REFRESH_TOKEN_ID to refreshTokenId,
            FIELD_CREATED_AT to now.toString(),
            FIELD_LAST_ACTIVITY_AT to now.toString(),
        )

        redisTemplate.opsForHash<String, String>().putAll(key, payload)
        redisTemplate.expire(key, ttl())

        return AuthSessionRecord(
            sessionId = sessionId,
            userId = userId,
            organizationId = organizationId,
            role = role,
            refreshTokenId = refreshTokenId,
            createdAt = now,
            lastActivityAt = now,
        )
    }

    fun get(sessionId: String): AuthSessionRecord? {
        val entries = redisTemplate.opsForHash<String, String>().entries(buildKey(sessionId))
        if (entries.isEmpty()) {
            return null
        }

        val userId = entries[FIELD_USER_ID]?.let(UUID::fromString) ?: return null
        val role = entries[FIELD_ROLE]?.let(Role::valueOf) ?: return null
        val refreshTokenId = entries[FIELD_REFRESH_TOKEN_ID] ?: return null
        val createdAt = entries[FIELD_CREATED_AT]?.let(Instant::parse) ?: return null
        val lastActivityAt = entries[FIELD_LAST_ACTIVITY_AT]?.let(Instant::parse) ?: return null
        val organizationId = entries[FIELD_ORGANIZATION_ID]
            ?.takeIf { it.isNotBlank() }
            ?.let(UUID::fromString)

        return AuthSessionRecord(
            sessionId = sessionId,
            userId = userId,
            organizationId = organizationId,
            role = role,
            refreshTokenId = refreshTokenId,
            createdAt = createdAt,
            lastActivityAt = lastActivityAt,
        )
    }

    fun touch(sessionId: String): AuthSessionRecord? {
        val current = get(sessionId) ?: return null
        val now = Instant.now()
        val key = buildKey(sessionId)

        redisTemplate.opsForHash<String, String>().put(key, FIELD_LAST_ACTIVITY_AT, now.toString())
        redisTemplate.expire(key, ttl())

        return current.copy(lastActivityAt = now)
    }

    fun rotateRefreshToken(sessionId: String, refreshTokenId: String): AuthSessionRecord? {
        val current = get(sessionId) ?: return null
        val now = Instant.now()
        val key = buildKey(sessionId)

        redisTemplate.opsForHash<String, String>().put(key, FIELD_REFRESH_TOKEN_ID, refreshTokenId)
        redisTemplate.opsForHash<String, String>().put(key, FIELD_LAST_ACTIVITY_AT, now.toString())
        redisTemplate.expire(key, ttl())

        return current.copy(refreshTokenId = refreshTokenId, lastActivityAt = now)
    }

    fun revoke(sessionId: String) {
        redisTemplate.delete(buildKey(sessionId))
    }

    private fun buildKey(sessionId: String): String = PREFIX + sessionId

    private fun ttl(): Duration = Duration.ofMillis(idleTimeoutMs.coerceAtLeast(1_000))
}
