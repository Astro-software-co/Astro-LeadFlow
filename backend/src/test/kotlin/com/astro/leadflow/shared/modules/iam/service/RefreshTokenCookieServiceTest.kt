package com.astro.leadflow.shared.modules.iam.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class RefreshTokenCookieServiceTest {

    private val cookieService = RefreshTokenCookieService(
        cookieName = "astro_refresh_token",
        cookiePath = "/api/v1/auth",
        sameSite = "Lax",
        secureByDefault = false,
        refreshExpirationMs = 604_800_000,
    )

    @Test
    fun `write should create secure httpOnly cookie header`() {
        val response = MockHttpServletResponse()

        cookieService.write(response, "refresh-token", secureRequest = true)

        val setCookie = requireNotNull(response.getHeader("Set-Cookie"))
        assertEquals(true, setCookie.contains("astro_refresh_token=refresh-token"))
        assertEquals(true, setCookie.contains("HttpOnly"))
        assertEquals(true, setCookie.contains("Secure"))
        assertEquals(true, setCookie.contains("SameSite=Lax"))
        assertEquals(true, setCookie.contains("Path=/api/v1/auth"))
    }

    @Test
    fun `clear should expire cookie immediately`() {
        val response = MockHttpServletResponse()

        cookieService.clear(response, secureRequest = false)

        val setCookie = requireNotNull(response.getHeader("Set-Cookie"))
        assertEquals(true, setCookie.contains("astro_refresh_token="))
        assertEquals(true, setCookie.contains("Max-Age=0"))
    }

    @Test
    fun `extract should return cookie value when present`() {
        val request = MockHttpServletRequest()
        request.addHeader("Cookie", "astro_refresh_token=cookie-refresh")
        request.setCookies(jakarta.servlet.http.Cookie("astro_refresh_token", "cookie-refresh"))

        assertEquals("cookie-refresh", cookieService.extract(request))
    }

    @Test
    fun `extract should return null when cookie is absent`() {
        val request = MockHttpServletRequest()

        assertNull(cookieService.extract(request))
    }
}
