package com.astro.leadflow.shared.modules.saas.service

import com.astro.leadflow.shared.modules.saas.api.responses.MarketplaceUsageDetail
import com.astro.leadflow.shared.modules.saas.api.responses.UsageSummaryResponse
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceConfigRepository
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import com.astro.leadflow.shared.modules.saas.domain.UsageLog
import com.astro.leadflow.shared.modules.saas.domain.UsageLogRepository
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional(readOnly = true)
    fun canSendMessage(organizationId: UUID, marketplaceName: String? = null): Boolean {
        val now = LocalDate.now()

        // 1. Verifica subscription ativa
        val subscription = subscriptionRepository.findByOrganizationId(organizationId)
            .orElse(null) ?: return false

        if (!subscription.isActive()) return false

        // 2. Verifica limite global do plano (regra soberana)
        val globalUsed = usageLogRepository.sumMessagesByOrganizationAndPeriod(
            organizationId, now.monthValue, now.year
        )
        val globalLimit = subscription.plan.messageLimit
        if (globalLimit != -1 && globalUsed >= globalLimit) return false

        // 3. Se não foi informado marketplace, só o global importa
        if (marketplaceName == null) return true

        // 4. Verifica se o marketplace de origem está habilitado
        val config = marketplaceConfigRepository
            .findByOrganizationIdAndMarketplaceName(organizationId, marketplaceName)
            .orElse(null) ?: return false

        if (!config.enabled) return false

        // 5. Verifica quota individual do marketplace de origem
        val marketplaceUsed = usageLogRepository
            .findByOrganizationIdAndMarketplaceNameAndPeriodMonthAndPeriodYear(
                organizationId, marketplaceName, now.monthValue, now.year
            )
            .map { it.messagesSent.toLong() }
            .orElse(0L)

        val marketplaceLimit = config.monthlyLimit
        if (marketplaceLimit == -1 || marketplaceUsed < marketplaceLimit) return true

        // 6. Marketplace de origem estourou — notifica admin e tenta redistribuição
        log.warn(
            "Marketplace {} da organização {} atingiu o limite mensal ({}/{}). " +
            "Tentando redistribuição para outros marketplaces ativos.",
            marketplaceName, organizationId, marketplaceUsed, marketplaceLimit
        )

        val outroComSaldo = marketplaceConfigRepository
            .findAllByOrganizationId(organizationId)
            .filter { it.enabled && it.marketplaceName != marketplaceName }
            .any { outro ->
                val outroUsed = usageLogRepository
                    .findByOrganizationIdAndMarketplaceNameAndPeriodMonthAndPeriodYear(
                        organizationId, outro.marketplaceName, now.monthValue, now.year
                    )
                    .map { it.messagesSent.toLong() }
                    .orElse(0L)
                outro.monthlyLimit == -1 || outroUsed < outro.monthlyLimit
            }

        if (!outroComSaldo) {
            log.warn(
                "Organização {} sem saldo em nenhum marketplace ativo. Envio bloqueado.",
                organizationId
            )
        }

        return outroComSaldo
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