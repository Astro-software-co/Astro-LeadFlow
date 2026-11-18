package com.astro.leadflow.shared.config

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.UUID

class ApiKeyAuthFilterTest {

    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var filterChain: FilterChain
    private lateinit var filter: ApiKeyAuthFilter

    @BeforeEach
    fun setup() {
        organizationRepository = mockk(relaxed = true)
        filterChain = mockk(relaxed = true)
        filter = ApiKeyAuthFilter(organizationRepository)
    }

    @Test
    fun `should return 401 when X-Api-Key header is missing`() {
        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/scraper")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertEquals(401, response.status)
        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should return 401 when X-Api-Key is invalid`() {
        every { organizationRepository.findByApiKey("invalid-key") } returns null

        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/scraper")
        request.addHeader("X-Api-Key", "invalid-key")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertEquals(401, response.status)
        verify(exactly = 0) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should set organization attribute and continue when API key is valid`() {
        val organizationId = UUID.randomUUID()
        val organization = Organization(
            id = organizationId,
            name = "Astro Test Org",
            slug = "astro-test-org",
            apiKey = "valid-key",
        )
        every { organizationRepository.findByApiKey("valid-key") } returns organization

        val request = MockHttpServletRequest("POST", "/api/v1/webhooks/scraper")
        request.addHeader("X-Api-Key", "valid-key")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertEquals(200, response.status)
        assertEquals(organizationId, request.getAttribute(ApiKeyAuthFilter.ORG_ID_ATTRIBUTE))
        verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    }
}
