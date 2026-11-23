package com.astro.leadflow.shared.modules.monitoring.controller

import com.astro.leadflow.shared.modules.monitoring.controller.dto.SystemHealthResponse
import com.astro.leadflow.shared.modules.monitoring.service.SystemHealthService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.security.access.prepost.PreAuthorize

class SystemHealthControllerTest {

    private val service = mockk<SystemHealthService>()
    private val controller = SystemHealthController(service)

    @Test
    fun `health returns masked operational flags from service`() {
        val response = SystemHealthResponse(
            abacatePayConfigured = true,
            metaWebhookSecretConfigured = true,
            metaApiTokenConfigured = false,
            redisUp = true,
            dbUp = true,
            chatbotEngineMode = "mock",
            awsLexConfigured = false,
            awsBedrockConfigured = false,
            publicApiUrlConfigured = true
        )
        every { service.current() } returns response

        assertEquals(response, controller.health())
    }

    @Test
    fun `controller is protected by ASTRO_ADMIN role`() {
        val annotation = SystemHealthController::class.java.getAnnotation(PreAuthorize::class.java)

        assertNotNull(annotation)
        assertEquals("hasRole('ASTRO_ADMIN')", annotation.value)
    }
}
