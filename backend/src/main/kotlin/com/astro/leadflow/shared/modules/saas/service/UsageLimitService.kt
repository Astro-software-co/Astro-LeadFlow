package com.astro.leadflow.shared.modules.saas.service

import com.astro.leadflow.shared.modules.saas.api.responses.MarketplaceUsageDetail
import com.astro.leadflow.shared.modules.saas.api.responses.UsageSummaryResponse
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceConfigRepository
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import com.astro.leadflow.shared.modules.saas.domain.UsageLog
import com.astro.leadflow.shared.modules.saas.domain.UsageLogRepository
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class UsageLimitService(
    private val subscriptionRepository: SubscriptionRepository,
    private val marketplaceConfigRepository: MarketplaceConfigRepository,
    private val usageLogRepository: UsageLogRepository
) {

    @Transactional(readOnly = true)
    fun canSendMessage(organizationId: UUID, marketplaceName: String? = null): Boolean {
        val now = LocalDate.now()
        val subscription = subscriptionRepository.findByOrganizationId(organizationId)
            .orElse(null) ?: return false

        if (!subscription.isActive()) return false

        val globalUsed = usageLogRepository.sumMessagesByOrganizationAndPeriod(
            organizationId, now.monthValue, now.year
        )
        if (globalUsed >= subscription.plan.messageLimit) return false

        if (marketplaceName != null) {
            val config = marketplaceConfigRepository
                .findByOrganizationIdAndMarketplaceName(organizationId, marketplaceName)
                .orElse(null) ?: return false

            if (!config.enabled) return false

            val marketplaceUsed = usageLogRepository
                .findByOrganizationIdAndMarketplaceNameAndPeriodMonthAndPeriodYear(
                    organizationId, marketplaceName, now.monthValue, now.year
                )
                .map { it.messagesSent.toLong() }
                .orElse(0L)

            if (marketplaceUsed >= config.monthlyLimit) return false
        }

        return true
    }

    @Transactional
    fun incrementUsage(organizationId: UUID, marketplaceName: String? = null) {
        val now = LocalDate.now()
        val organization = subscriptionRepository.findByOrganizationId(organizationId)
            .orElseThrow { ResourceNotFoundException("Subscription", organizationId) }
            .organization

        incrementOrCreate(organizationId, null, now.monthValue, now.year, organization)
        if (marketplaceName != null) {
            incrementOrCreate(organizationId, marketplaceName, now.monthValue, now.year, organization)
        }
    }

    @Transactional(readOnly = true)
    fun getUsageSummary(organizationId: UUID): UsageSummaryResponse {
        val now = LocalDate.now()
        val subscription = subscriptionRepository.findByOrganizationId(organizationId)
            .orElseThrow { ResourceNotFoundException("Subscription", organizationId) }

        val totalUsed = usageLogRepository.sumMessagesByOrganizationAndPeriod(
            organizationId, now.monthValue, now.year
        )

        val marketplaceUsages = marketplaceConfigRepository
            .findAllByOrganizationId(organizationId)
            .map { config ->
                val used = usageLogRepository
                    .findByOrganizationIdAndMarketplaceNameAndPeriodMonthAndPeriodYear(
                        organizationId, config.marketplaceName, now.monthValue, now.year
                    )
                    .map { it.messagesSent.toLong() }
                    .orElse(0L)

                MarketplaceUsageDetail(
                    marketplaceName = config.marketplaceName,
                    enabled = config.enabled,
                    used = used,
                    limit = config.monthlyLimit.toLong(),
                    remaining = maxOf(0L, config.monthlyLimit - used)
                )
            }

        return UsageSummaryResponse(
            planName = subscription.plan.name,
            globalLimit = subscription.plan.messageLimit.toLong(),
            globalUsed = totalUsed,
            globalRemaining = maxOf(0L, subscription.plan.messageLimit - totalUsed),
            renewsAt = subscription.renewsAt,
            marketplaces = marketplaceUsages
        )
    }

    private fun incrementOrCreate(
        organizationId: UUID,
        marketplaceName: String?,
        month: Int,
        year: Int,
        organization: com.astro.leadflow.shared.modules.iam.domain.Organization
    ) {
        val updated = usageLogRepository.incrementCount(organizationId, marketplaceName, month, year)
        if (updated == 0) {
            usageLogRepository.save(
                UsageLog(
                    organization = organization,
                    marketplaceName = marketplaceName,
                    messagesSent = 1,
                    periodMonth = month,
                    periodYear = year
                )
            )
        }
    }
}