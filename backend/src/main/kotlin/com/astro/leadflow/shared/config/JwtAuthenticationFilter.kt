package com.astro.leadflow.shared.config

import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.modules.iam.service.TokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val tokenService: TokenService,
    private val userRepository: UserRepository,
    private val authSessionService: com.astro.leadflow.shared.modules.iam.service.AuthSessionService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)

        if (!tokenService.isTokenValid(token) || tokenService.isRefreshToken(token)) {
            filterChain.doFilter(request, response)
            return
        }

        val sessionId = tokenService.extractSessionId(token)
        if (sessionId.isNullOrBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        val userId = tokenService.extractUserId(token)
        val session = authSessionService.touch(sessionId)

        if (session == null || session.userId != userId) {
            filterChain.doFilter(request, response)
            return
        }

        val user = userRepository.findByIdWithOrganization(userId).orElse(null)

        if (user != null && user.isEnabled) {
            val auth = UsernamePasswordAuthenticationToken(
                user,
                null,
                user.authorities
            )
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }
}
