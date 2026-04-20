package com.astro.leadflow.shared.modules.iam.service

import com.astro.leadflow.shared.exception.BusinessException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RefreshTokenCookieService(
    @Value("\${app.auth.refresh-cookie.name:astro_refresh_token}")
    private val cookieName: String,
    @Value("\${app.auth.refresh-cookie.path:/api/v1/auth}")
    private val cookiePath: String,
    @Value("\${app.auth.refresh-cookie.same-site:Lax}")
    private val sameSite: String,
    @Value("\${app.auth.refresh-cookie.secure:false}")
    private val secureByDefault: Boolean,
    @Value("\${app.jwt.refresh-expiration-ms:604800000}")
    private val refreshExpirationMs: Long,
) {

    fun write(response: HttpServletResponse, refreshToken: String, secureRequest: Boolean) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(secureByDefault || secureRequest)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(Duration.ofMillis(refreshExpirationMs))
                .build()
                .toString(),
        )
    }

    fun clear(response: HttpServletResponse, secureRequest: Boolean) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secureByDefault || secureRequest)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(Duration.ZERO)
                .build()
                .toString(),
        )
    }

    fun extract(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == cookieName }
            ?.value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    fun resolveOrThrow(request: HttpServletRequest, fallback: String? = null): String =
        fallback?.takeIf { it.isNotBlank() }
            ?: extract(request)
            ?: throw BusinessException("Refresh token ausente ou inválido.")
}
