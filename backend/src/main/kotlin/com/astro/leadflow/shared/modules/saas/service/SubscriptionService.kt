package com.astro.leadflow.shared.modules.saas.service

import com.astro.leadflow.shared.exception.AccessDeniedException
import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.saas.api.responses.BillingHistoryResponse
import com.astro.leadflow.shared.modules.saas.api.responses.MarketplaceConfigResponse
import com.astro.leadflow.shared.modules.saas.api.responses.SubscriptionResponse
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceConfig
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceConfigRepository
import com.astro.leadflow.shared.modules.saas.domain.PlanRepository
import com.astro.leadflow.shared.modules.saas.domain.BillingHistoryEntryRepository
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionStatus
import com.astro.leadflow.shared.modules.saas.domain.UsageLogRepository
import com.astro.leadflow.shared.modules.saas.mapper.SaasMapper
import com.astro.leadflow.shared.util.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val planRepository: PlanRepository,
    private val marketplaceConfigRepository: MarketplaceConfigRepository,
    private val organizationRepository: OrganizationRepository,
    private val billingHistoryEntryRepository: BillingHistoryEntryRepository,
    private val usageLogRepository: UsageLogRepository,
    private val saasMapper: SaasMapper
) {

    @Transactional(readOnly = true)
    fun getMySubscription(): SubscriptionResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val subscription = subscriptionRepository.findByOrganizationId(orgId)
            .orElseThrow { ResourceNotFoundException("Subscription", orgId) }
        return saasMapper.toSubscriptionResponse(subscription)
    }

    @Transactional(readOnly = true)
    fun listBillingHistory(): List<BillingHistoryResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val subscription = subscriptionRepository.findByOrganizationId(orgId)
            .orElseThrow { ResourceNotFoundException("Subscription", orgId) }

        val providerHistory = billingHistoryEntryRepository
            .findAllBySubscriptionIdOrderByBilledAtDescCreatedAtDesc(subscription.id)

        if (providerHistory.isNotEmpty()) {
            return providerHistory.map { entry ->
                BillingHistoryResponse(
                    id = entry.id,
                    billedAt = entry.billedAt,
                    issuedAt = entry.issuedAt,
                    description = entry.description,
                    amountBrl = entry.amountBrl,
                    status = entry.status,
                    invoiceUrl = entry.invoiceUrl,
                )
            }
        }

        val usageTimeline = usageLogRepository
            .findAllByOrganizationIdAndMarketplaceNameIsNullOrderByPeriodYearDescPeriodMonthDescCreatedAtDesc(orgId)

        return usageTimeline.map { usage ->
            val billedAt = LocalDate.of(usage.periodYear, usage.periodMonth, 1)
            BillingHistoryResponse(
                id = usage.id,
                billedAt = billedAt,
                issuedAt = usage.createdAt.toLocalDate(),
                description = subscription.plan.displayName() + " - " + "%02d/%04d".format(usage.periodMonth, usage.periodYear),
                amountBrl = subscription.plan.priceBrl,
                status = toBillingStatus(subscription.status),
                invoiceUrl = null,
            )
        }
    }

    @Transactional
    fun changePlan(planId: UUID): SubscriptionResponse {
        if (!planRepository.existsById(planId)) {
            throw ResourceNotFoundException("Plan", planId)
        }
        throw InvalidOperationException("Troca de plano deve ser confirmada via checkout Abacate Pay e webhook de pagamento.")
    }

    @Transactional
    fun addMarketplace(marketplaceName: String, monthlyLimit: Int): MarketplaceConfigResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val organization = organizationRepository.findById(orgId)
            .orElseThrow { ResourceNotFoundException("Organization", orgId) }
        val subscription = subscriptionRepository.findByOrganizationId(orgId)
            .orElseThrow { ResourceNotFoundException("Subscription", orgId) }

        val currentCount = marketplaceConfigRepository.countByOrganizationId(orgId)
        if (currentCount >= subscription.plan.marketplaceLimit) {
            throw BusinessException(
                "Limite de marketplaces do plano '" + subscription.plan.name + "' atingido (" + subscription.plan.marketplaceLimit + " marketplaces)."
            )
        }

        val normalized = marketplaceName.uppercase().trim()
        if (marketplaceConfigRepository
                .findByOrganizationIdAndMarketplaceName(orgId, normalized).isPresent
        ) {
            throw BusinessException("Marketplace '" + normalized + "' já está cadastrado.")
        }

        val config = marketplaceConfigRepository.save(
            MarketplaceConfig(
                organization = organization,
                marketplaceName = normalized,
                monthlyLimit = monthlyLimit,
            )
        )
        return saasMapper.toMarketplaceResponse(config)
    }

    @Transactional
    fun updateMarketplace(
        configId: UUID,
        enabled: Boolean?,
        monthlyLimit: Int?,
    ): MarketplaceConfigResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val config = marketplaceConfigRepository.findById(configId)
            .orElseThrow { ResourceNotFoundException("MarketplaceConfig", configId) }
        if (config.organization.id != orgId) throw AccessDeniedException()
        enabled?.let { config.enabled = it }
        monthlyLimit?.let { config.monthlyLimit = it }
        return saasMapper.toMarketplaceResponse(marketplaceConfigRepository.save(config))
    }

    @Transactional
    fun removeMarketplace(configId: UUID) {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val config = marketplaceConfigRepository.findById(configId)
            .orElseThrow { ResourceNotFoundException("MarketplaceConfig", configId) }
        if (config.organization.id != orgId) throw AccessDeniedException()
        marketplaceConfigRepository.delete(config)
    }

    @Transactional(readOnly = true)
    fun listMarketplaces(): List<MarketplaceConfigResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return marketplaceConfigRepository.findAllByOrganizationId(orgId)
            .map { saasMapper.toMarketplaceResponse(it) }
    }

    private fun toBillingStatus(status: SubscriptionStatus): String =
        when (status) {
            SubscriptionStatus.ACTIVE -> "PAID"
            SubscriptionStatus.PAST_DUE -> "PENDING"
            SubscriptionStatus.SUSPENDED, SubscriptionStatus.CANCELLED -> "FAILED"
        }
}
