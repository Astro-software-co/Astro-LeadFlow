package com.astro.leadflow.shared.modules.monitoring.service

import com.astro.leadflow.shared.modules.monitoring.controller.dto.SystemHealthResponse
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.info.BuildProperties

class HealthAggregatorServiceTest {
    @Test
    fun `overview combines health and metrics`() {
        val health = mockk<SystemHealthService>()
        val metrics = mockk<PerformanceMetricsService>()
        val buildProperties = mockk<ObjectProvider<BuildProperties>>()
        every { health.current() } returns SystemHealthResponse(true, true, true, true, true, "mock", false, false, true)
        every { metrics.overviewRequestsPerMinute() } returns 10.0
        every { metrics.overviewErrorRatePercent() } returns 1.0
        every { metrics.overviewP95LatencyMs() } returns 50.0
        every { buildProperties.getIfAvailable() } returns null

        val overview = HealthAggregatorService(health, metrics, buildProperties).overview()

        assertEquals(10.0, overview.requestsPerMinute)
        assertEquals("mock", overview.health.chatbotEngineMode)
    }
}
