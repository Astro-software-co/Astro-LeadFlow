package com.astro.leadflow.shared.modules.integrations.webmotors

import com.astro.leadflow.shared.config.ApiKeyAuthFilter
import com.astro.leadflow.shared.config.JwtAuthenticationFilter
import com.astro.leadflow.shared.config.SecurityConfig
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.iam.domain.Role
import com.astro.leadflow.shared.modules.iam.domain.User
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.modules.iam.service.AuthSessionService
import com.astro.leadflow.shared.modules.iam.service.TokenService
import com.astro.leadflow.shared.modules.integrations.webmotors.api.WebmotorsController
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsAccountService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsDecisionService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsInboxService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsSyncCoordinator
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(WebmotorsController::class)
@Import(SecurityConfig::class, JwtAuthenticationFilter::class, ApiKeyAuthFilter::class)
@TestPropertySource(properties = ["app.rate-limit.enabled=false"])
class WebmotorsControllerSecurityTest {

    @Autowired lateinit var mvc: MockMvc

    @MockBean lateinit var accounts: WebmotorsAccountService
    @MockBean lateinit var inbox: WebmotorsInboxService
    @MockBean lateinit var decisions: WebmotorsDecisionService
    @MockBean lateinit var coordinator: WebmotorsSyncCoordinator
    @MockBean lateinit var tokenService: TokenService
    @MockBean lateinit var authSessionService: AuthSessionService
    @MockBean lateinit var userRepository: UserRepository
    @MockBean lateinit var organizationRepository: OrganizationRepository
    @MockBean lateinit var redisTemplate: RedisTemplate<String, Any>

    private val org = Organization(name = "Loja A", slug = "loja-a")

    private fun user(role: Role, organization: Organization? = org) = User(
        organization = organization, name = "U", email = "u-${role.name}@test.local",
        passwordHash = "x", role = role
    )

    private fun auth(u: User) = authentication(UsernamePasswordAuthenticationToken(u, null, u.authorities))

    @Test
    fun `sem token responde 401`() {
        mvc.perform(get("/api/v1/integrations/webmotors/proposals")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `qualquer API protegida sem token responde 401 (entry point global)`() {
        mvc.perform(get("/api/v1/leads")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `SELLER responde 403`() {
        mvc.perform(get("/api/v1/integrations/webmotors/proposals").with(auth(user(Role.SELLER))))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `ASTRO_ADMIN sem organizacao responde 403`() {
        mvc.perform(get("/api/v1/integrations/webmotors/proposals").with(auth(user(Role.ASTRO_ADMIN, null))))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `MANAGER lista as propostas da propria organizacao`() {
        `when`(inbox.list(org.id, null)).thenReturn(emptyList())
        mvc.perform(get("/api/v1/integrations/webmotors/proposals").with(auth(user(Role.MANAGER))))
            .andExpect(status().isOk)
    }

    @Test
    fun `MANAGER recebe 404 ao aceitar proposta de outra organizacao`() {
        val foreign = UUID.randomUUID()
        `when`(decisions.accept(org.id, foreign)).thenThrow(ResourceNotFoundException("Proposta Webmotors", foreign))
        mvc.perform(post("/api/v1/integrations/webmotors/proposals/$foreign/accept").with(auth(user(Role.MANAGER))))
            .andExpect(status().isNotFound)
    }
}
