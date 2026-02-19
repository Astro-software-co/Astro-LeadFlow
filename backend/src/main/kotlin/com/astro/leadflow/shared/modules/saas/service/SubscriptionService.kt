package com.astro.leadflow.modules.saas.service

import com.astro.leadflow.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.modules.saas.domain.*
import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
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
    private val organizationRepository: OrganizationRepository
) {

    @Transactional(readOnly = true)
    fun getMySubscription(): SubscriptionResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val subscription = subscriptionRepository.findByOrganizationId(orgId)
            .orElseThrow { ResourceNotFoundException("Subscription", orgId) }
        return SubscriptionResponse.from(subscription)
    }

    @Transactional
    fun changePlan(planId: UUID): SubscriptionResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val subscription = subscriptionRepository.findByOrganizationId(orgId)
            .orElseThrow { ResourceNotFoundException("Subscription", orgId) }

        val newPlan = planRepository.findById(planId)
            .orElseThrow { ResourceNotFoundException("Plan", planId) }

        subscription.plan = newPlan
        subscriptionRepository.save(subscription)

        return SubscriptionResponse.from(subscription)
    }

    // ── Marketplace Configs ──────────────────────────────────

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
                "Limite de marketplaces do plano '${subscription.plan.name}' atingido " +
                "(${subscription.plan.marketplaceLimit} marketplaces)."
            )
        }

        val normalized = marketplaceName.uppercase().trim()

        if (marketplaceConfigRepository
                .findByOrganizationIdAndMarketplaceName(orgId, normalized).isPresent) {
            throw BusinessException("Marketplace '$normalized' já está cadastrado.")
        }

        val config = marketplaceConfigRepository.save(
            MarketplaceConfig(
                organization = organization,
                marketplaceName = normalized,
                monthlyLimit = monthlyLimit
            )
        )

        return MarketplaceConfigResponse.from(config)
    }

    @Transactional
    fun updateMarketplace(
        configId: UUID,
        enabled: Boolean?,
        monthlyLimit: Int?
    ): MarketplaceConfigResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val config = marketplaceConfigRepository.findById(configId)
            .orElseThrow { ResourceNotFoundException("MarketplaceConfig", configId) }

        if (config.organization.id != orgId) {
            throw com.astro.leadflow.shared.exception.AccessDeniedException()
        }

        enabled?.let { config.enabled = it }
        monthlyLimit?.let { config.monthlyLimit = it }

        return MarketplaceConfigResponse.from(marketplaceConfigRepository.save(config))
    }

    @Transactional
    fun removeMarketplace(configId: UUID) {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val config = marketplaceConfigRepository.findById(configId)
            .orElseThrow { ResourceNotFoundException("MarketplaceConfig", configId) }

        if (config.organization.id != orgId) {
            throw com.astro.leadflow.shared.exception.AccessDeniedException()
        }

        marketplaceConfigRepository.delete(config)
    }

    @Transactional(readOnly = true)
    fun listMarketplaces(): List<MarketplaceConfigResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return marketplaceConfigRepository.findAllByOrganizationId(orgId)
            .map { MarketplaceConfigResponse.from(it) }
    }
}

// ─── Response DTOs ──────────────────────────────────────────

data class SubscriptionResponse(
    val planId: UUID,
    val planName: String,
    val monthlyPriceBrl: java.math.BigDecimal,
    val messageLimit: Int,
    val marketplaceLimit: Int,
    val status: SubscriptionStatus,
    val renewsAt: LocalDate?
) {
    companion object {
        fun from(s: Subscription) = SubscriptionResponse(
            planId = s.plan.id,
            planName = s.plan.name,
            monthlyPriceBrl = s.plan.monthlyPriceBrl,
            messageLimit = s.plan.messageLimit,
            marketplaceLimit = s.plan.marketplaceLimit,
            status = s.status,
            renewsAt = s.renewsAt
        )
    }
}

data class MarketplaceConfigResponse(
    val id: UUID,
    val marketplaceName: String,
    val enabled: Boolean,
    val monthlyLimit: Int
) {
    companion object {
        fun from(c: MarketplaceConfig) = MarketplaceConfigResponse(
            id = c.id,
            marketplaceName = c.marketplaceName,
            enabled = c.enabled,
            monthlyLimit = c.monthlyLimit
        )
    }
}