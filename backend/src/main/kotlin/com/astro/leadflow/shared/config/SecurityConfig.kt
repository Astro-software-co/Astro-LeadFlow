package com.astro.leadflow.shared.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val apiKeyAuthFilter: ApiKeyAuthFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth
                    // ── Swagger / OpenAPI ──────────────────────────
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/swagger-resources/**",
                        "/webjars/**"
                    ).permitAll()

                    // ── Auth público ───────────────────────────────
                    .requestMatchers("/api/v1/auth/**").permitAll()

                    // ── Webhooks (protegidos por API Key no Gateway)
                    .requestMatchers("/api/v1/webhooks/**").permitAll()

                    // ── Onboarding público ─────────────────────────
                    .requestMatchers(HttpMethod.POST, "/api/onboarding").permitAll()

                    // ── Planos públicos (landing page) ─────────────
                    .requestMatchers(HttpMethod.GET, "/api/v1/plans").permitAll()

                    // ── Actuator (health público, demais restritos) ─
                    .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                    .requestMatchers("/actuator/**").hasRole("ASTRO_ADMIN")

                    // ── Admin Astro (onboarding + finance dashboard + referral summary)
                    .requestMatchers("/api/v1/admin/**").hasRole("ASTRO_ADMIN")
                    .requestMatchers("/api/admin/**").hasRole("ASTRO_ADMIN")

                    // ── Referral (somente MANAGER) ─────────────────
                    .requestMatchers("/api/referral/**").hasRole("MANAGER")
                    .requestMatchers("/api/v1/referral/**").hasRole("MANAGER")

                    // ── Perfil do usuario autenticado ──────────────
                    .requestMatchers(
                        "/api/v1/users/me",
                        "/api/v1/users/me/**"
                    ).authenticated()

                    // ── Integrações de marketplace (somente MANAGER) ─
                    .requestMatchers("/api/v1/integrations/**").hasRole("MANAGER")
                    .requestMatchers("/api/v1/privacy/**").hasRole("MANAGER")

                    // ── APIs de concessionária ─────────────────────
                    .requestMatchers(
                        "/api/v1/users/**",
                        "/api/v1/leads/**",
                        "/api/v1/kanban/**",
                        "/api/v1/vehicles/**",
                        "/api/v1/chat/**",
                        "/api/v1/saas/**",
                        "/api/v1/analytics/**",
                        "/api/v1/referrals/**"
                    ).hasRole("DEALERSHIP")

                    // ── Operações restritas ────────────────────────
                    .requestMatchers(HttpMethod.DELETE, "/**")
                    .hasRole("MANAGER")

                    // ── Tudo mais requer autenticação ──────────────
                    .anyRequest().authenticated()
            }
            // Sem token → 401 (sem isso o Spring responde 403); 403 fica para papel sem permissão.
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
            .addFilterBefore(
                apiKeyAuthFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(
        config: AuthenticationConfiguration
    ): AuthenticationManager = config.authenticationManager
}
