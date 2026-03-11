package com.astro.leadflow.shared.modules.saas.mapper

import com.astro.leadflow.shared.modules.saas.api.responses.MarketplaceConfigResponse
import com.astro.leadflow.shared.modules.saas.api.responses.PlanResponse
import com.astro.leadflow.shared.modules.saas.api.responses.SubscriptionResponse
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceConfig
import com.astro.leadflow.shared.modules.saas.domain.Plan
import com.astro.leadflow.shared.modules.saas.domain.Subscription
import org.springframework.stereotype.Component

@Component
class SaasMapper {

    fun toSubscriptionResponse(subscription: Subscription): SubscriptionResponse =
        SubscriptionResponse(
            planId = subscription.plan.id,
            planName = subscription.plan.name,
            monthlyPriceBrl = subscription.plan.monthlyPriceBrl,
            messageLimit = subscription.plan.messageLimit,
            marketplaceLimit = subscription.plan.marketplaceLimit,
            status = subscription.status,
            renewsAt = subscription.renewsAt
        )

    fun toMarketplaceResponse(config: MarketplaceConfig): MarketplaceConfigResponse =
        MarketplaceConfigResponse(
            id = config.id,
            marketplaceName = config.marketplaceName,
            enabled = config.enabled,
            monthlyLimit = config.monthlyLimit
        )

    fun toPlanResponse(plan: Plan): PlanResponse =
        PlanResponse(
            id = plan.id,
            name = plan.name,
            monthlyPriceBrl = plan.monthlyPriceBrl,
            messageLimit = plan.messageLimit,
            marketplaceLimit = plan.marketplaceLimit
        )
}