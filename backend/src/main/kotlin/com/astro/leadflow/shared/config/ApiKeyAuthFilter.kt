package com.astro.leadflow.shared.config

import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ApiKeyAuthFilter(
    private val organizationRepository: OrganizationRepository
) : OncePerRequestFilter() {

    companion object {
        const val HEADER_NAME = "X-Api-Key"
        const val ORG_ID_ATTRIBUTE = "organizationId"
        private const val SCRAPER_PATH = "/api/v1/webhooks/scraper"
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return !request.requestURI.startsWith(SCRAPER_PATH)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val apiKey = request.getHeader(HEADER_NAME)

        if (apiKey.isNullOrBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Header X-Api-Key ausente")
            return
        }

        val organization = organizationRepository.findByApiKey(apiKey)
        if (organization == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API Key inválida")
            return
        }

        request.setAttribute(ORG_ID_ATTRIBUTE, organization.id)
        filterChain.doFilter(request, response)
    }
}
