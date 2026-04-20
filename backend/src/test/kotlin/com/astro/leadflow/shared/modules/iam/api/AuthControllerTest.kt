package com.astro.leadflow.shared.modules.iam.api

import com.astro.leadflow.shared.modules.iam.api.requests.LoginRequest
import com.astro.leadflow.shared.modules.iam.api.responses.TokenResponse
import com.astro.leadflow.shared.modules.iam.service.AuthService
import com.astro.leadflow.shared.modules.iam.service.RefreshTokenCookieService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class AuthControllerTest {

    @MockK lateinit var authService: AuthService
    @MockK lateinit var refreshTokenCookieService: RefreshTokenCookieService

    private lateinit var controller: AuthController

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        controller = AuthController(authService, refreshTokenCookieService)
    }

    @Test
    fun `login should write refresh token cookie and strip token from response body`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val login = LoginRequest(email = "admin@astro.test", password = "Senha123!")
        val tokenResponse = TokenResponse(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            pendingTwoFactor = false,
        )

        every { authService.login(login) } returns tokenResponse
        every { refreshTokenCookieService.write(response, "refresh-1", false) } just runs

        val result = controller.login(login, response, request)

        assertEquals("access-1", result.accessToken)
        assertEquals(null, result.refreshToken)
        verify(exactly = 1) { refreshTokenCookieService.write(response, "refresh-1", false) }
    }

    @Test
    fun `refresh should resolve token from cookie when request body is absent`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val tokenResponse = TokenResponse(
            accessToken = "access-2",
            refreshToken = "refresh-2",
            pendingTwoFactor = false,
        )

        every { refreshTokenCookieService.resolveOrThrow(request, null) } returns "cookie-refresh"
        every { authService.refreshToken("cookie-refresh") } returns tokenResponse
        every { refreshTokenCookieService.write(response, "refresh-2", false) } just runs

        val result = controller.refresh(null, response, request)

        assertEquals("access-2", result.accessToken)
        assertEquals(null, result.refreshToken)
        verify(exactly = 1) { authService.refreshToken("cookie-refresh") }
    }

    @Test
    fun `logout should revoke token resolved from cookie and clear cookie`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        every { refreshTokenCookieService.resolveOrThrow(request, null) } returns "cookie-refresh"
        every { authService.logout("cookie-refresh") } just runs
        every { refreshTokenCookieService.clear(response, false) } just runs

        controller.logout(null, response, request)

        verify(exactly = 1) { authService.logout("cookie-refresh") }
        verify(exactly = 1) { refreshTokenCookieService.clear(response, false) }
    }
}
