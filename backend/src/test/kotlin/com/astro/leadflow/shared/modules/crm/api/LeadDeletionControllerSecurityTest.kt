package com.astro.leadflow.shared.modules.crm.api

import com.astro.leadflow.shared.config.ApiKeyAuthFilter
import com.astro.leadflow.shared.config.JwtAuthenticationFilter
import com.astro.leadflow.shared.config.SecurityConfig
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.crm.service.LeadDeletionResult
import com.astro.leadflow.shared.modules.crm.service.LeadDeletionService
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.iam.domain.Role
import com.astro.leadflow.shared.modules.iam.domain.User
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.modules.iam.service.AuthSessionService
import com.astro.leadflow.shared.modules.iam.service.TokenService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(LeadDeletionController::class)
@Import(SecurityConfig::class, JwtAuthenticationFilter::class, ApiKeyAuthFilter::class)
@TestPropertySource(properties = ["app.rate-limit.enabled=false"])
class LeadDeletionControllerSecurityTest {

    @Autowired lateinit var mvc: MockMvc

    @MockBean lateinit var deletion: LeadDeletionService
    @MockBean lateinit var tokenService: TokenService
    @MockBean lateinit var authSessionService: AuthSessionService
    @MockBean lateinit var userRepository: UserRepository
    @MockBean lateinit var organizationRepository: OrganizationRepository
    @MockBean lateinit var redisTemplate: RedisTemplate<String, Any>

    private val org = Organization(name = "Loja A", slug = "loja-a")
    private fun user(role: Role) = User(organization = org, name = "U", email = "u-$role@test.local", passwordHash = "x", role = role)
    private fun auth(u: User) = authentication(UsernamePasswordAuthenticationToken(u, null, u.authorities))

    @Test
    fun `MANAGER exclui com includeSamePhone e recebe 204`() {
        val manager = user(Role.MANAGER)
        val id = UUID.randomUUID()
        `when`(deletion.delete(org.id, id, true, manager.id, "duplicado")).thenReturn(LeadDeletionResult(2, 3))

        mvc.perform(delete("/api/v1/leads/$id?includeSamePhone=true&reason=duplicado").with(auth(manager)))
            .andExpect(status().isNoContent)
        verify(deletion).delete(org.id, id, true, manager.id, "duplicado")
    }

    @Test
    fun `SELLER recebe 403 e sem token 401`() {
        val id = UUID.randomUUID()
        mvc.perform(delete("/api/v1/leads/$id").with(auth(user(Role.SELLER)))).andExpect(status().isForbidden)
        mvc.perform(delete("/api/v1/leads/$id")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `lead de outra organizacao responde 404`() {
        val manager = user(Role.MANAGER)
        val id = UUID.randomUUID()
        `when`(deletion.delete(org.id, id, false, manager.id, null)).thenThrow(ResourceNotFoundException("Lead", id))

        mvc.perform(delete("/api/v1/leads/$id").with(auth(manager))).andExpect(status().isNotFound)
    }
}
