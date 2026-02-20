package com.astro.leadflow.modules.iam.service

import com.astro.leadflow.modules.iam.api.requests.*
import com.astro.leadflow.modules.iam.api.responses.TokenResponse
import com.astro.leadflow.modules.iam.domain.*
import com.astro.leadflow.shared.exception.*
import com.astro.leadflow.shared.modules.iam.service.TwoFactorService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val tokenService: TokenService,
    private val passwordEncoder: PasswordEncoder,
    private val twoFactorService: TwoFactorService
) {

    @Transactional
    fun registerOrganization(request: RegisterOrganizationRequest): TokenResponse {
        if (organizationRepository.existsBySlug(request.slug))
            throw DuplicateResourceException("Organization", "slug", request.slug)
        if (userRepository.existsByEmail(request.adminEmail))
            throw DuplicateResourceException("User", "email", request.adminEmail)

        val organization = organizationRepository.save(
            Organization(name = request.organizationName, slug = request.slug)
        )
        val admin = userRepository.save(
            User(
                organization = organization,
                name = request.adminName,
                email = request.adminEmail,
                passwordHash = passwordEncoder.encode(request.password),
                role = Role.ADMIN
            )
        )
        return buildTokenResponse(admin)
    }

    fun login(request: LoginRequest): TokenResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { BusinessException("Credenciais inválidas.") }

        if (!passwordEncoder.matches(request.password, user.password))
            throw BusinessException("Credenciais inválidas.")

        if (user.status == UserStatus.SUSPENDED)
            throw BusinessException("Conta suspensa. Entre em contato com o administrador.")

        if (user.status == UserStatus.PENDING_VERIFICATION)
            throw BusinessException("Conta pendente de ativação.")

        if (user.twoFactorEmailEnabled || user.twoFactorSmsEnabled) {
            twoFactorService.sendCode(user)
            return TokenResponse(pendingTwoFactor = true)
        }

        return buildTokenResponse(user)
    }

    fun verify2FA(request: Verify2FARequest): TokenResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { ResourceNotFoundException("User", request.email) }

        if (user.twoFactorCode != request.code)
            throw BusinessException("Código 2FA inválido.")

        if (user.twoFactorCodeExpiresAt?.isBefore(LocalDateTime.now()) == true)
            throw BusinessException("Código 2FA expirado. Solicite um novo.")

        user.twoFactorCode = null
        user.twoFactorCodeExpiresAt = null
        userRepository.save(user)

        return buildTokenResponse(user)
    }

    fun refreshToken(refreshToken: String): TokenResponse {
        if (!tokenService.isTokenValid(refreshToken) || !tokenService.isRefreshToken(refreshToken))
            throw BusinessException("Refresh token inválido ou expirado.")

        val userId = tokenService.extractUserId(refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User", userId) }

        if (!user.isEnabled)
            throw BusinessException("Conta inativa ou suspensa.")

        return buildTokenResponse(user)
    }

    private fun buildTokenResponse(user: User) = TokenResponse(
        accessToken = tokenService.generateAccessToken(user),
        refreshToken = tokenService.generateRefreshToken(user),
        userId = user.id,
        role = user.role.name,
        organizationId = user.organization?.id
    )
}