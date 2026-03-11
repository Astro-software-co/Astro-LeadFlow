package com.astro.leadflow.shared.modules.iam.api

import com.astro.leadflow.shared.modules.iam.api.requests.LoginRequest
import com.astro.leadflow.shared.modules.iam.api.requests.RefreshTokenRequest
import com.astro.leadflow.shared.modules.iam.api.requests.RegisterOrganizationRequest
import com.astro.leadflow.shared.modules.iam.api.requests.Verify2FARequest
import com.astro.leadflow.shared.modules.iam.api.responses.TokenResponse
import com.astro.leadflow.shared.modules.iam.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "IAM — Auth", description = "Autenticação e controle de acesso")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Registra nova organização",
        description = "Cria a organização e o usuário ADMIN. Retorna tokens JWT."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Organização criada com sucesso"),
        ApiResponse(responseCode = "400", description = "Dados inválidos"),
        ApiResponse(responseCode = "409", description = "Slug ou e-mail já cadastrado")
    )
    fun register(@Valid @RequestBody request: RegisterOrganizationRequest): TokenResponse =
        authService.registerOrganization(request)

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
    fun login(@Valid @RequestBody request: LoginRequest): TokenResponse =
        authService.login(request)

    @PostMapping("/2fa/verify")
    @Operation(
        summary = "Verifica código 2FA",
        description = "Valida o código enviado por e-mail ou SMS e retorna os tokens JWT."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Código válido, tokens retornados"),
        ApiResponse(responseCode = "400", description = "Código inválido ou expirado")
    )
    fun verify2FA(@Valid @RequestBody request: Verify2FARequest): TokenResponse =
        authService.verify2FA(request)

    @PostMapping("/refresh")
    @Operation(
        summary = "Renova access token",
        description = "Usa o refresh token para gerar um novo access token."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Token renovado com sucesso"),
        ApiResponse(responseCode = "400", description = "Refresh token inválido ou expirado")
    )
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): TokenResponse =
        authService.refreshToken(request.refreshToken)
}