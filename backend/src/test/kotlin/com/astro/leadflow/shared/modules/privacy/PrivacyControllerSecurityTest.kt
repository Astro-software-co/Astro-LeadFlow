package com.astro.leadflow.shared.modules.privacy

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
import com.astro.leadflow.shared.modules.privacy.api.PrivacyController
import com.astro.leadflow.shared.modules.privacy.api.requests.ExecuteDataSubjectRequestRequest
import com.astro.leadflow.shared.modules.privacy.service.DataSubjectRequestService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(PrivacyController::class)
@Import(SecurityConfig::class, JwtAuthenticationFilter::class, ApiKeyAuthFilter::class)
@TestPropertySource(properties = ["app.rate-limit.enabled=false"])
class PrivacyControllerSecurityTest {

    @Autowired lateinit var mvc: MockMvc

    @MockBean lateinit var service: DataSubjectRequestService
    @MockBean lateinit var tokenService: TokenService
    @MockBean lateinit var authSessionService: AuthSessionService
    @MockBean lateinit var userRepository: UserRepository
    @MockBean lateinit var organizationRepository: OrganizationRepository
    @MockBean lateinit var redisTemplate: RedisTemplate<String, Any>

    private val org = Organization(name = "Loja A", slug = "loja-a")
    private fun user(role: Role) = User(organization = org, name = "U", email = "u-$role@test.local", passwordHash = "x", role = role)
    private fun auth(u: User) = authentication(UsernamePasswordAuthenticationToken(u, null, u.authorities))

    @Test
    fun `sem token 401 e SELLER 403`() {
        mvc.perform(get("/api/v1/privacy/requests")).andExpect(status().isUnauthorized)
        mvc.perform(get("/api/v1/privacy/requests").with(auth(user(Role.SELLER)))).andExpect(status().isForbidden)
    }

    @Test
    fun `MANAGER registra solicitacao e validacao barra corpo incompleto`() {
        val manager = user(Role.MANAGER)
        mvc.perform(
            post("/api/v1/privacy/requests").with(auth(manager)).contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"ANONYMIZATION","identifier":"11987654321","channel":"EMAIL"}""")
        ).andExpect(status().isCreated)
        mvc.perform(
            post("/api/v1/privacy/requests").with(auth(manager)).contentType(MediaType.APPLICATION_JSON).content("""{"type":"ANONYMIZATION"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `solicitacao de outra organizacao responde 404`() {
        val manager = user(Role.MANAGER)
        val id = UUID.randomUUID()
        `when`(service.execute(org.id, id, ExecuteDataSubjectRequestRequest("11987654321"), manager.id))
            .thenThrow(ResourceNotFoundException("Solicitação de titular", id))

        mvc.perform(
            post("/api/v1/privacy/requests/$id/execute").with(auth(manager)).contentType(MediaType.APPLICATION_JSON)
                .content("""{"identifier":"11987654321"}""")
        ).andExpect(status().isNotFound)
    }
}
