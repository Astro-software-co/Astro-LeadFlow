package com.astro.leadflow.modules.saas.service

import com.astro.leadflow.modules.saas.domain.*
import com.astro.leadflow.shared.exception.BusinessException
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

    /**
     * Verifica se a organização pode enviar mais mensagens.
     * Checa tanto o limite global da org quanto o limite do marketplace.
     */
    @Transactional(readOnly = true)
    fun canSendMessage(organizationId: UUID, marketplaceName: String? = null): Boolean {
        val now = LocalDate.now()
        val subscription = subscriptionRepository.findByOrganizationId(organizationId)
            .orElse(null) ?: return false

        if (!subscription.isActive()) return false

        val globalLimit = subscription.plan.messageLimit
        val globalUsed = usageLogRepository.sumMessagesByOrganizationAndPeriod(
            organizationId, now.monthValue, now.year
        )

        // Verifica limite global da organização
        if (globalUsed >= globalLimit) return false

        // Verifica limite específico do marketplace
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

    /**
     * Incrementa contadores de uso após envio bem-sucedido.
     * Cria registro do mês se não existir ainda.
     */
    @Transactional
    fun incrementUsage(organizationId: UUID, marketplaceName: String? = null) {
        val now = LocalDate.now()
        val organization = subscriptionRepository.findByOrganizationId(organizationId)
            .orElseThrow { ResourceNotFoundException("Subscription", organizationId) }
            .organization

        // Incrementa log global da organização
        incrementOrCreate(organizationId, null, now.monthValue, now.year, organization)

        // Incrementa log do marketplace específico
        if (marketplaceName != null) {
            incrementOrCreate(organizationId, marketplaceName, now.monthValue, now.year, organization)
        }
    }

    /**
     * Retorna resumo de uso do mês atual para exibir no painel Marketing.
     */
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
        organization: com.astro.leadflow.modules.iam.domain.Organization
    ) {
        val updated = usageLogRepository.incrementCount(organizationId, marketplaceName, month, year)

        if (updated == 0) {
            // Registro ainda não existe para este mês — cria
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

// ─── Response DTOs (internos ao módulo saas) ───────────────

data class UsageSummaryResponse(
    val planName: String,
    val globalLimit: Long,
    val globalUsed: Long,
    val globalRemaining: Long,
    val renewsAt: java.time.LocalDate?,
    val marketplaces: List<MarketplaceUsageDetail>
)

data class MarketplaceUsageDetail(
    val marketplaceName: String,
    val enabled: Boolean,
    val used: Long,
    val limit: Long,
    val remaining: Long
)