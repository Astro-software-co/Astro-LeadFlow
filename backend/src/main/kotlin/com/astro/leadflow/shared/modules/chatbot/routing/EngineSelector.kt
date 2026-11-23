package com.astro.leadflow.shared.modules.chatbot.routing

import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.saas.domain.AiEngineTier
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EngineSelector(
    private val subscriptionRepository: SubscriptionRepository
) {
    fun select(organizationId: UUID): EngineSelection {
        val tier = subscriptionRepository.findByOrganizationId(organizationId)
            .map { it.plan.aiEngineTier }
            .orElse(AiEngineTier.NONE)

        return when (tier) {
            AiEngineTier.NONE -> EngineSelection(EngineType.TEMPLATE, allowBedrockFallback = false)
            AiEngineTier.LEX_ONLY -> EngineSelection(EngineType.LEX, allowBedrockFallback = false)
            AiEngineTier.LEX_BEDROCK_FALLBACK -> EngineSelection(EngineType.LEX, allowBedrockFallback = true)
        }
    }
}
