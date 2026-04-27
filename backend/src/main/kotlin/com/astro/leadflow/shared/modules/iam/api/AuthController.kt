package com.astro.leadflow.shared.modules.iam.api

import com.astro.leadflow.shared.modules.iam.api.requests.LoginRequest
import com.astro.leadflow.shared.modules.iam.api.requests.RefreshTokenRequest
import com.astro.leadflow.shared.modules.iam.api.requests.RegisterOrganizationRequest
import com.astro.leadflow.shared.modules.iam.api.requests.Verify2FARequest
import com.astro.leadflow.shared.modules.iam.api.responses.TokenResponse
import com.astro.leadflow.shared.modules.iam.service.AuthService
import com.astro.leadflow.shared.modules.iam.service.RefreshTokenCookieService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "IAM — Auth", description = "Autenticação e controle de acesso")
class AuthController(
    private val authService: AuthService,
    private val refreshTokenCookieService: RefreshTokenCookieService,
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Registra nova organização",
        description = "Cria a organização e o usuário MANAGER. Retorna tokens JWT."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Organização criada com sucesso"),
        ApiResponse(responseCode = "400", description = "Dados inválidos"),
        ApiResponse(responseCode = "409", description = "Slug ou e-mail já cadastrado")
    )
    fun register(
        @Valid @RequestBody request: RegisterOrganizationRequest,
        response: HttpServletResponse,
        servletRequest: HttpServletRequest,
    ): TokenResponse =
        authService.registerOrganization(request)
            .attachRefreshCookie(response, servletRequest)

    @PostMapping("/login")
    @Operation(
        summary = "Realiza login",
        description = "Retorna tokens JWT. Se 2FA estiver ativo, retorna pendingTwoFactor=true."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        ApiResponse(responseCode = "400", description = "Credenciais inválidas"),
        ApiResponse(responseCode = "401", description = "Conta suspensa ou pendente")
    )
    fun login(
        @Valid @RequestBody request: LoginRequest,
        response: HttpServletResponse,
        servletRequest: HttpServletRequest,
    ): TokenResponse =
        authService.login(request)
            .attachRefreshCookie(response, servletRequest)

    @PostMapping("/2fa/verify")
    @Operation(
        summary = "Verifica código 2FA",
        description = "Valida o código enviado por e-mail ou SMS e retorna os tokens JWT."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Código válido, tokens retornados"),
        ApiResponse(responseCode = "400", description = "Código inválido ou expirado")
    )
    fun verify2FA(
        @Valid @RequestBody request: Verify2FARequest,
        response: HttpServletResponse,
        servletRequest: HttpServletRequest,
    ): TokenResponse =
        authService.verify2FA(request)
            .attachRefreshCookie(response, servletRequest)

    @PostMapping("/refresh")
    @Operation(
        summary = "Renova access token",
        description = "Usa o refresh token para gerar um novo access token."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Token renovado com sucesso"),
        ApiResponse(responseCode = "400", description = "Refresh token inválido ou expirado")
    )
    fun refresh(
        @RequestBody(required = false) request: RefreshTokenRequest?,
        response: HttpServletResponse,
        servletRequest: HttpServletRequest,
    ): TokenResponse {
        val refreshToken = refreshTokenCookieService.resolveOrThrow(servletRequest, request?.refreshToken)
        return authService.refreshToken(refreshToken)
            .attachRefreshCookie(response, servletRequest)
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Realiza logout",
        description = "Revoga o refresh token informado para impedir novos refresh."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Logout realizado com sucesso"),
        ApiResponse(responseCode = "400", description = "Refresh token inválido ou expirado")
    )
    fun logout(
        @RequestBody(required = false) request: RefreshTokenRequest?,
        response: HttpServletResponse,
        servletRequest: HttpServletRequest,
    ) {
        val refreshToken = refreshTokenCookieService.resolveOrThrow(servletRequest, request?.refreshToken)
        authService.logout(refreshToken)
        refreshTokenCookieService.clear(response, servletRequest.isSecure)
    }

    private fun TokenResponse.attachRefreshCookie(
        response: HttpServletResponse,
        request: HttpServletRequest,
    ): TokenResponse {
        val refreshToken = refreshToken ?: return this
        refreshTokenCookieService.write(response, refreshToken, request.isSecure)
        return copy(refreshToken = null)
    }
}
