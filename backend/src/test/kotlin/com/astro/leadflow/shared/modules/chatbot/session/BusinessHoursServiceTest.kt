package com.astro.leadflow.shared.modules.chatbot.session

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class BusinessHoursServiceTest {

    private val service = BusinessHoursService()

    @Test
    fun `isOpen returns true from 9 inclusive to 18 exclusive Sao Paulo time`() {
        assertTrue(service.isOpen(ZonedDateTime.parse("2026-04-27T09:00:00-03:00[America/Sao_Paulo]")))
        assertTrue(service.isOpen(ZonedDateTime.parse("2026-04-27T17:59:00-03:00[America/Sao_Paulo]")))
    }

    @Test
    fun `isOpen returns false outside business hours`() {
        assertFalse(service.isOpen(ZonedDateTime.parse("2026-04-27T08:59:00-03:00[America/Sao_Paulo]")))
        assertFalse(service.isOpen(ZonedDateTime.parse("2026-04-27T18:00:00-03:00[America/Sao_Paulo]")))
    }
}
