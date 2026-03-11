package com.astro.leadflow.shared.modules.saas.service

import com.astro.leadflow.shared.modules.saas.api.responses.MarketplaceConfigResponse
import com.astro.leadflow.shared.modules.saas.api.responses.SubscriptionResponse
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceConfig
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceConfigRepository
import com.astro.leadflow.shared.modules.saas.domain.PlanRepository
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import com.astro.leadflow.shared.modules.saas.mapper.SaasMapper
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.exception.AccessDeniedException
import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.util.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val planRepository: PlanRepository,
    private val marketplaceConfigRepository: MarketplaceConfigRepository,
    private val organizationRepository: OrganizationRepository,
    private val saasMapper: SaasMapper
) {

    @Transactional(readOnly = true)
    fun getMySubscription(): SubscriptionResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val subscription = subscriptionRepository.findByOrganizationId(orgId)
            .orElseThrow { ResourceNotFoundException("Subscription", orgId) }
        return saasMapper.toSubscriptionResponse(subscription)
    }

    @Transactional
    fun changePlan(planId: UUID): SubscriptionResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val subscription = subscriptionRepository.findByOrganizationId(orgId)
            .orElseThrow { ResourceNotFoundException("Subscription", orgId) }
        val newPlan = planRepository.findById(planId)
            .orElseThrow { ResourceNotFoundException("Plan", planId) }
        subscription.plan = newPlan
        return saasMapper.toSubscriptionResponse(subscriptionRepository.save(subscription))
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
        return saasMapper.toMarketplaceResponse(config)
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
}