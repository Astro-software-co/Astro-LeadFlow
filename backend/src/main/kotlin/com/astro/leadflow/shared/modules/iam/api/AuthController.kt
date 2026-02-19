package com.astro.leadflow.modules.iam.api

import com.astro.leadflow.modules.iam.api.requests.*
import com.astro.leadflow.modules.iam.api.responses.TokenResponse
import com.astro.leadflow.modules.iam.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Autenticação e controle de acesso")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra nova organização")
    fun register(@Valid @RequestBody request: RegisterOrganizationRequest): TokenResponse =
        authService.registerOrganization(request)

    @PostMapping("/login")
    @Operation(summary = "Login")
    fun login(@Valid @RequestBody request: LoginRequest): TokenResponse =
        authService.login(request)

    @PostMapping("/2fa/verify")
    @Operation(summary = "Verifica código 2FA")
    fun verify2FA(@Valid @RequestBody request: Verify2FARequest): TokenResponse =
        authService.verify2FA(request)

    @PostMapping("/refresh")
    @Operation(summary = "Renova access token")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): TokenResponse =
        authService.refreshToken(request.refreshToken)
}