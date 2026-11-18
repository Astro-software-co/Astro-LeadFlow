package com.astro.leadflow.shared.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig(
    @Value("\${app.cors.allowed-origins:}")
    private val allowedOrigins: String,
    @Value("\${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private val allowedOriginPatterns: String,
) {

    @Bean
    fun corsFilter(): CorsFilter {
        val config = CorsConfiguration()
        parseCsv(allowedOrigins).takeIf { it.isNotEmpty() }?.let { config.allowedOrigins = it }
        parseCsv(allowedOriginPatterns).takeIf { it.isNotEmpty() }?.let { config.allowedOriginPatterns = it }
        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true
        config.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", config)

        return CorsFilter(source)
    }

    private fun parseCsv(value: String): List<String> =
        value.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
