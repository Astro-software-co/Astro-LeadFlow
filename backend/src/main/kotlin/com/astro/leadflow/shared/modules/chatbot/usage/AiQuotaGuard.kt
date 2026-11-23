package com.astro.leadflow.shared.modules.chatbot.usage

import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AiQuotaGuard(
    private val subscriptionRepository: SubscriptionRepository,
    private val aiTokenUsageRepository: AiTokenUsageRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    fun canUseTokens(organizationId: UUID, requestedTokens: Int = 1): Boolean {
        val subscription = subscriptionRepository.findByOrganizationId(organizationId).orElse(null) ?: return false
        val maxTokens = subscription.plan.maxAiTokensPerMonth ?: return true
        if (maxTokens <= 0) return false

        val used = usedThisMonth(organizationId).toInt()
        val projected = used + requestedTokens
        if (projected >= maxTokens) return false
        if (projected >= maxTokens * WARNING_PERCENT / 100) {
            eventPublisher.publishEvent(
                BotQuotaWarningEvent(
                    organizationId = organizationId,
                    organizationName = subscription.organization.name,
                    usedTokens = projected,
                    maxTokens = maxTokens
                )
            )
        }
        return true
    }

    fun usedThisMonth(organizationId: UUID): Long {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val from = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
        val to = from.plusMonths(1)
        return aiTokenUsageRepository.sumTokensByOrganizationAndPeriod(organizationId, from, to)
    }

    companion object {
        private const val WARNING_PERCENT = 80
    }
}
