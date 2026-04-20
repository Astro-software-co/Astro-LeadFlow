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

    @Value("\${app.jwt.expiration-ms:3600000}")
    private val expirationMs: Long,

    @Value("\${app.jwt.refresh-expiration-ms:604800000}")
    private val refreshExpirationMs: Long
) {
    private val signingKey by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun generateAccessToken(user: User): String =
        buildToken(user, expirationMs, isRefresh = false)

    fun generateRefreshToken(user: User): String =
        buildToken(user, refreshExpirationMs, isRefresh = true)

    private fun buildToken(user: User, expiration: Long, isRefresh: Boolean = false): String {
        val now = Date()
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role.name)
            .claim("organizationId", user.organization?.id?.toString())
            .claim("refresh", isRefresh)
            .issuedAt(now)
            .expiration(Date(now.time + expiration))
            .signWith(signingKey)
            .compact()
    }

    fun extractUserId(token: String): UUID =
        UUID.fromString(extractAllClaims(token).subject)

    fun extractOrganizationId(token: String): UUID? =
        extractAllClaims(token)["organizationId"]
            ?.toString()
            ?.let { UUID.fromString(it) }

    fun extractExpiration(token: String): Instant =
        extractAllClaims(token).expiration.toInstant()

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
