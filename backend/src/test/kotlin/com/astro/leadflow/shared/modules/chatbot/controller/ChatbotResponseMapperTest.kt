package com.astro.leadflow.shared.modules.chatbot.controller

import com.astro.leadflow.shared.modules.chatbot.domain.entity.BotSession
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.iam.domain.Organization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChatbotResponseMapperTest {
    @Test
    fun `toSummary maps session fields`() {
        val organization = Organization(name = "Astro", slug = "astro")
        val lead = Lead(organization = organization, phoneNumber = "+5511999999999", origin = "TEST", name = "Lead")
        val session = BotSession(organization = organization, lead = lead).apply {
            currentIntent = BotIntent.GREETING
            engineUsed = EngineType.LEX
            fallbackCount = 1
        }

        val response = ChatbotResponseMapper().toSummary(session)

        assertEquals(session.id, response.id)
        assertEquals("Lead", response.leadName)
        assertEquals(BotIntent.GREETING, response.currentIntent)
    }
}
