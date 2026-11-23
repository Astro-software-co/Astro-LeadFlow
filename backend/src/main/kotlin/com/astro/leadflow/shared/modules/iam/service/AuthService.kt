package com.astro.leadflow.shared.modules.iam.service

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.DuplicateResourceException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.iam.api.requests.LoginRequest
import com.astro.leadflow.shared.modules.iam.api.requests.RegisterOrganizationRequest
import com.astro.leadflow.shared.modules.iam.api.requests.Verify2FARequest
import com.astro.leadflow.shared.modules.iam.api.responses.TokenResponse
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.iam.domain.Role
import com.astro.leadflow.shared.modules.iam.domain.User
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.modules.iam.domain.UserStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val tokenService: TokenService,
    private val passwordEncoder: PasswordEncoder,
    private val twoFactorService: TwoFactorService,
    private val refreshTokenBlocklistService: RefreshTokenBlocklistService,
    private val authSessionService: AuthSessionService,
    private val userReferralCodeService: UserReferralCodeService,
) {

    @Transactional
    fun registerOrganization(request: RegisterOrganizationRequest): TokenResponse {
        if (organizationRepository.existsBySlug(request.slug)) {
            throw DuplicateResourceException("Organization", "slug", request.slug)
        }
        if (userRepository.existsByEmail(request.adminEmail)) {
            throw DuplicateResourceException("User", "email", request.adminEmail)
        }

        val organization = organizationRepository.save(
            Organization(name = request.organizationName, slug = request.slug)
        )
        val manager = userRepository.save(
            User(
                organization = organization,
                name = request.adminName,
                email = request.adminEmail,
                passwordHash = passwordEncoder.encode(request.password),
                role = Role.MANAGER,
            )
        )
        userReferralCodeService.ensureReferralCode(manager)
        return issueTokensForNewSession(manager)
    }

    @Transactional
    fun login(request: LoginRequest): TokenResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { BusinessException("Credenciais inválidas.") }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw BusinessException("Credenciais inválidas.")
        }

        if (user.status == UserStatus.SUSPENDED) {
            throw BusinessException("Conta suspensa. Entre em contato com o administrador.")
        }

        if (user.status == UserStatus.PENDING_VERIFICATION) {
            throw BusinessException("Conta pendente de ativação.")
        }

        if (user.twoFactorEmailEnabled || user.twoFactorSmsEnabled) {
            twoFactorService.sendCode(user)
            return TokenResponse(pendingTwoFactor = true)
        }

        return issueTokensForNewSession(user)
    }

    fun verify2FA(request: Verify2FARequest): TokenResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { ResourceNotFoundException("User", request.email) }

        if (user.twoFactorCode != request.code) {
            throw BusinessException("Código 2FA inválido.")
        }

        if (user.twoFactorCodeExpiresAt?.isBefore(LocalDateTime.now()) == true) {
            throw BusinessException("Código 2FA expirado. Solicite um novo.")
        }

        user.twoFactorCode = null
        user.twoFactorCodeExpiresAt = null
        userRepository.save(user)

        return issueTokensForNewSession(user)
    }

    @Transactional
    fun refreshToken(refreshToken: String): TokenResponse {
        if (!tokenService.isTokenValid(refreshToken) || !tokenService.isRefreshToken(refreshToken)) {
            throw BusinessException("Refresh token inválido ou expirado.")
        }

        if (refreshTokenBlocklistService.isRevoked(refreshToken)) {
            throw BusinessException("Refresh token inválido ou expirado.")
        }

        val sessionId = tokenService.extractSessionId(refreshToken)
            ?: throw BusinessException("Refresh token inválido ou expirado.")
        val refreshTokenId = tokenService.extractTokenId(refreshToken)
            ?: throw BusinessException("Refresh token inválido ou expirado.")
        val session = authSessionService.get(sessionId)
            ?: throw BusinessException("Refresh token inválido ou expirado.")
        val userId = tokenService.extractUserId(refreshToken)

        if (session.userId != userId || session.refreshTokenId != refreshTokenId) {
            throw BusinessException("Refresh token inválido ou expirado.")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User", userId) }

        if (!user.isEnabled) {
            throw BusinessException("Conta inativa ou suspensa.")
        }

        refreshTokenBlocklistService.revoke(refreshToken, tokenService.extractExpiration(refreshToken))
        return rotateSessionTokens(user, sessionId)
    }

    @Transactional
    fun logout(refreshToken: String) {
        if (!tokenService.isTokenValid(refreshToken) || !tokenService.isRefreshToken(refreshToken)) {
            throw BusinessException("Refresh token inválido ou expirado.")
        }

        val expiresAt = tokenService.extractExpiration(refreshToken)
        tokenService.extractSessionId(refreshToken)?.let(authSessionService::revoke)
        refreshTokenBlocklistService.revoke(refreshToken, expiresAt)
    }

    private fun issueTokensForNewSession(user: User): TokenResponse {
        val sessionId = UUID.randomUUID().toString()
        return issueTokens(user, sessionId, createSession = true)
    }

    private fun rotateSessionTokens(user: User, sessionId: String): TokenResponse =
        issueTokens(user, sessionId, createSession = false)

    private fun issueTokens(user: User, sessionId: String, createSession: Boolean): TokenResponse {
        val refreshTokenId = UUID.randomUUID().toString()
        val accessToken = tokenService.generateAccessToken(user, sessionId)
        val refreshToken = tokenService.generateRefreshToken(user, sessionId, refreshTokenId)

        if (createSession) {
            authSessionService.create(
                sessionId = sessionId,
                userId = user.id,
                organizationId = user.organization?.id,
                role = user.role,
                refreshTokenId = refreshTokenId,
            )
        } else {
            authSessionService.rotateRefreshToken(sessionId, refreshTokenId)
                ?: throw BusinessException("Sessão expirada ou inválida.")
        }

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user.id,
            role = user.role.name,
            organizationId = user.organization?.id,
        )
    }
}
