package com.astro.leadflow.shared.modules.chatbot.usage

import com.astro.leadflow.shared.modules.chatbot.domain.entity.BotSession
import com.astro.leadflow.shared.modules.chatbot.domain.entity.AiTokenUsage
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.TokenUsage
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.saas.domain.UsageLog
import com.astro.leadflow.shared.modules.saas.domain.UsageLogRepository
import com.astro.leadflow.shared.modules.saas.domain.UsageMetricType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class AiTokenTrackerTest {
    private val aiRepository = mockk<AiTokenUsageRepository>(relaxed = true)
    private val usageRepository = mockk<UsageLogRepository>(relaxed = true)
    private val tracker = AiTokenTracker(aiRepository, usageRepository)
    private val organization = Organization(name = "Astro", slug = "astro")
    private val lead = Lead(organization = organization, phoneNumber = "+5511999999999", origin = "TEST")
    private val session = BotSession(organization = organization, lead = lead)

    @Test
    fun `track persists token usage and usage log metrics`() {
        every { usageRepository.incrementMetricCount(any(), any(), any(), any(), any(), any()) } returns 0
        every { aiRepository.save(any<AiTokenUsage>()) } answers { firstArg() }
        every { usageRepository.save(any<UsageLog>()) } answers { firstArg() }

        tracker.track(session, EngineType.BEDROCK, TokenUsage(10, 20))

        verify { aiRepository.save(match { it.tokensInput == 10 && it.tokensOutput == 20 }) }
        verify { usageRepository.save(match { it.metricType == UsageMetricType.AI_TOKENS_INPUT && it.messagesSent == 10 }) }
        verify { usageRepository.save(match { it.metricType == UsageMetricType.AI_TOKENS_OUTPUT && it.messagesSent == 20 }) }
    }

    @Test
    fun `track ignores template and zero token usage`() {
        tracker.track(session, EngineType.TEMPLATE, TokenUsage(0, 0))

        verify(exactly = 0) { aiRepository.save(any()) }
    }
}
