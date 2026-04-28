package com.astro.leadflow.shared.modules.monitoring.service

import com.astro.leadflow.shared.modules.chatbot.session.BotSessionRepository
import com.astro.leadflow.shared.modules.chatbot.usage.AiTokenUsageRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChatbotInsightsServiceTest {
    @Test
    fun `insights returns aggregate values`() {
        val sessions = mockk<BotSessionRepository>()
        val tokens = mockk<AiTokenUsageRepository>()
        every { sessions.count() } returns 3
        every { tokens.count() } returns 5

        val response = ChatbotInsightsService(sessions, tokens).insights()

        assertEquals(3, response.totalSessions)
        assertEquals(5, response.tokensConsumed)
    }
}
