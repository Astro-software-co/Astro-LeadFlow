package com.astro.leadflow.shared.modules.iam.service

import com.astro.leadflow.modules.iam.api.requests.LoginRequest
import com.astro.leadflow.modules.iam.api.requests.RegisterOrganizationRequest
import com.astro.leadflow.modules.iam.domain.*
import com.astro.leadflow.modules.iam.service.AuthService
import com.astro.leadflow.modules.iam.service.TokenService
import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.DuplicateResourceException
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import java.util.UUID

class AuthServiceTest {

    @MockK lateinit var userRepository: UserRepository
    @MockK lateinit var organizationRepository: OrganizationRepository
    @MockK lateinit var tokenService: TokenService
    @MockK lateinit var passwordEncoder: PasswordEncoder
    @MockK lateinit var twoFactorService: TwoFactorService

    private lateinit var authService: AuthService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        authService = AuthService(
            userRepository,
            organizationRepository,
            tokenService,
            passwordEncoder,
            twoFactorService
        )
    }

    // ── registerOrganization ─────────────────────────────────

    @Test
    fun `registerOrganization should create org and admin user`() {
        val request = RegisterOrganizationRequest(
            organizationName = "Test Motors",
            slug = "test-motors",
            adminName = "Admin User",
            adminEmail = "admin@test.com",
            password = "senha123"
        )
        val org = Organization(name = request.organizationName, slug = request.slug)
        val user = buildUser(organization = org)

        every { organizationRepository.existsBySlug(request.slug) } returns false
        every { userRepository.existsByEmail(request.adminEmail) } returns false
        every { passwordEncoder.encode(request.password) } returns "hashed_password"
        every { organizationRepository.save(any()) } returns org
        every { userRepository.save(any()) } returns user
        every { tokenService.generateAccessToken(user) } returns "access_token"
        every { tokenService.generateRefreshToken(user) } returns "refresh_token"

        val result = authService.registerOrganization(request)

        Assertions.assertNotNull(result.accessToken)
        Assertions.assertEquals("access_token", result.accessToken)
        verify(exactly = 1) { organizationRepository.save(any()) }
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `registerOrganization should throw when slug already exists`() {
        val request = RegisterOrganizationRequest(
            organizationName = "Test",
            slug = "test",
            adminName = "Admin",
            adminEmail = "a@b.com",
            password = "123"
        )
        every { organizationRepository.existsBySlug(request.slug) } returns true

        assertThrows<DuplicateResourceException> {
            authService.registerOrganization(request)
        }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `registerOrganization should throw when email already exists`() {
        val request = RegisterOrganizationRequest(
            organizationName = "Test",
            slug = "test-new",
            adminName = "Admin",
            adminEmail = "existing@test.com",
            password = "123"
        )
        every { organizationRepository.existsBySlug(request.slug) } returns false
        every { userRepository.existsByEmail(request.adminEmail) } returns true

        assertThrows<DuplicateResourceException> {
            authService.registerOrganization(request)
        }
    }

    // ── login ────────────────────────────────────────────────

    @Test
    fun `login should return tokens when credentials are valid`() {
        val request = LoginRequest(email = "user@test.com", password = "senha123")
        val user = buildUser()

        every { userRepository.findByEmail(request.email) } returns Optional.of(user)
        every { passwordEncoder.matches(request.password, user.password) } returns true
        every { tokenService.generateAccessToken(user) } returns "access_token"
        every { tokenService.generateRefreshToken(user) } returns "refresh_token"

        val result = authService.login(request)

        Assertions.assertEquals("access_token", result.accessToken)
        Assertions.assertEquals("refresh_token", result.refreshToken)
        Assertions.assertTrue(!result.pendingTwoFactor)
    }

    @Test
    fun `login should throw when user not found`() {
        every { userRepository.findByEmail(any()) } returns Optional.empty()

        assertThrows<BusinessException> {
            authService.login(LoginRequest("x@x.com", "123"))
        }
    }

    @Test
    fun `login should throw when password is wrong`() {
        val user = buildUser()
        every { userRepository.findByEmail(any()) } returns Optional.of(user)
        every { passwordEncoder.matches(any(), any()) } returns false

        assertThrows<BusinessException> {
            authService.login(LoginRequest("user@test.com", "errada"))
        }
    }

    @Test
    fun `login should throw when user is suspended`() {
        val user = buildUser(status = UserStatus.SUSPENDED)
        every { userRepository.findByEmail(any()) } returns Optional.of(user)
        every { passwordEncoder.matches(any(), any()) } returns true

        assertThrows<BusinessException> {
            authService.login(LoginRequest("user@test.com", "senha123"))
        }
    }

    @Test
    fun `login should return pendingTwoFactor when 2FA is enabled`() {
        val user = buildUser(twoFactorEmailEnabled = true)
        every { userRepository.findByEmail(any()) } returns Optional.of(user)
        every { passwordEncoder.matches(any(), any()) } returns true
        every { twoFactorService.sendCode(user) } just Runs

        val result = authService.login(LoginRequest("user@test.com", "senha123"))

        Assertions.assertTrue(result.pendingTwoFactor)
        verify(exactly = 1) { twoFactorService.sendCode(user) }
    }

    // ── helpers ──────────────────────────────────────────────

    private fun buildUser(
        organization: Organization = Organization(
            name = "Test Org", slug = "test-org"
        ),
        status: UserStatus = UserStatus.ACTIVE,
        twoFactorEmailEnabled: Boolean = false
    ) = User(
        id = UUID.randomUUID(),
        organization = organization,
        name = "Test User",
        email = "user@test.com",
        passwordHash = "hashed_password",
        role = Role.ADMIN,
        status = status,
        twoFactorEmailEnabled = twoFactorEmailEnabled
    )
}
