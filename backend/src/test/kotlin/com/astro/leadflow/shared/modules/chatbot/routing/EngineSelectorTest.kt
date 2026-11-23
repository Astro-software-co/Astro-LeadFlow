package com.astro.leadflow.shared.modules.chatbot.routing

import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.saas.domain.AiEngineTier
import com.astro.leadflow.shared.modules.saas.domain.Plan
import com.astro.leadflow.shared.modules.saas.domain.Subscription
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class EngineSelectorTest {

    private val repository = mockk<SubscriptionRepository>()
    private val selector = EngineSelector(repository)
    private val organization = Organization(name = "Astro", slug = "astro")

    @Test
    fun `selects template when organization has no subscription`() {
        every { repository.findByOrganizationId(any()) } returns Optional.empty()

        val selection = selector.select(UUID.randomUUID())

        assertEquals(EngineType.TEMPLATE, selection.primary)
        assertFalse(selection.allowBedrockFallback)
    }

    @Test
    fun `selects lex only for profissional tier`() {
        every { repository.findByOrganizationId(any()) } returns Optional.of(subscription(AiEngineTier.LEX_ONLY))

        val selection = selector.select(UUID.randomUUID())

        assertEquals(EngineType.LEX, selection.primary)
        assertFalse(selection.allowBedrockFallback)
    }

    @Test
    fun `selects lex with bedrock fallback for enterprise tier`() {
        every { repository.findByOrganizationId(any()) } returns Optional.of(subscription(AiEngineTier.LEX_BEDROCK_FALLBACK))

        val selection = selector.select(UUID.randomUUID())

        assertEquals(EngineType.LEX, selection.primary)
        assertTrue(selection.allowBedrockFallback)
    }

    private fun subscription(tier: AiEngineTier): Subscription =
        Subscription(
            organization = organization,
            plan = Plan(
                name = "Plano",
                priceBrl = BigDecimal.TEN,
                messageLimit = 100,
                marketplaceLimit = 1,
                aiEngineTier = tier
            )
        )
}
