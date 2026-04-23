package com.astro.leadflow.shared.modules.iam.service

import com.astro.leadflow.shared.modules.iam.domain.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class TokenService(
    @Value("\${app.jwt.secret:local_dev_secret_key_must_be_at_least_32_chars_long}")
    private val secret: String,

    @Value("\${app.jwt.expiration-ms:300000}")
    private val expirationMs: Long,

    @Value("\${app.jwt.refresh-expiration-ms:900000}")
    private val refreshExpirationMs: Long
) {
    private val signingKey by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun generateAccessToken(user: User, sessionId: String): String =
        buildToken(user, expirationMs, sessionId = sessionId, isRefresh = false)

    fun generateRefreshToken(user: User, sessionId: String, refreshTokenId: String): String =
        buildToken(user, refreshExpirationMs, sessionId = sessionId, isRefresh = true, tokenId = refreshTokenId)

    private fun buildToken(
        user: User,
        expiration: Long,
        sessionId: String,
        isRefresh: Boolean = false,
        tokenId: String? = null,
    ): String {
        val now = Date()
        val builder = Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role.name)
            .claim("organizationId", user.organization?.id?.toString())
            .claim("sid", sessionId)
            .claim("refresh", isRefresh)
            .issuedAt(now)
            .expiration(Date(now.time + expiration))
            .signWith(signingKey)

        tokenId?.let(builder::id)

        return builder.compact()
    }

    fun extractUserId(token: String): UUID =
        UUID.fromString(extractAllClaims(token).subject)

    fun extractOrganizationId(token: String): UUID? =
        extractAllClaims(token)["organizationId"]
            ?.toString()
            ?.let { UUID.fromString(it) }

    fun extractExpiration(token: String): Instant =
        extractAllClaims(token).expiration.toInstant()

    fun extractSessionId(token: String): String? =
        extractAllClaims(token)["sid"]?.toString()

    fun extractTokenId(token: String): String? =
        extractAllClaims(token).id

    fun isTokenValid(token: String): Boolean = runCatching {
        extractAllClaims(token).expiration.after(Date())
    }.getOrDefault(false)

    fun isRefreshToken(token: String): Boolean =
        extractAllClaims(token)["refresh"] as? Boolean ?: false

    private fun extractAllClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
