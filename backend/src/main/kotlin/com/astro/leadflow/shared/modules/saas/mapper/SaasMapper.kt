package com.astro.leadflow.shared.modules.saas.mapper

import com.astro.leadflow.shared.modules.saas.api.responses.MarketplaceConfigResponse
import com.astro.leadflow.shared.modules.saas.api.responses.PlanResponse
import com.astro.leadflow.shared.modules.saas.api.responses.SubscriptionResponse
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceConfig
import com.astro.leadflow.shared.modules.saas.domain.Plan
import com.astro.leadflow.shared.modules.saas.domain.Subscription
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionStatus
import org.springframework.stereotype.Component

@Component
class SaasMapper {

    fun toSubscriptionResponse(subscription: Subscription): SubscriptionResponse =
        SubscriptionResponse(
            planId = subscription.plan.id,
            planName = subscription.plan.name,
            planDescription = subscription.plan.description,
            priceBrl = subscription.plan.priceBrl,
            billingCycle = subscription.plan.billingCycle,
            billingCycleLabel = subscription.plan.billingCycle.label,
            messageLimit = subscription.plan.messageLimit,
            marketplaceLimit = subscription.plan.marketplaceLimit,
            status = subscription.status,
            renewsAt = subscription.renewsAt,
            paymentProvider = subscription.paymentProvider,
            checkoutUrl = subscription.abacateCheckoutUrl,
            canCancel = !subscription.abacateSubscriptionId.isNullOrBlank() &&
                subscription.status != SubscriptionStatus.CANCELLED,
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
            code = plan.code,
            name = plan.name,
            description = plan.description,
            priceBrl = plan.priceBrl,
            billingCycle = plan.billingCycle,
            billingCycleLabel = plan.billingCycle.label,
            messageLimit = plan.messageLimit,
            marketplaceLimit = plan.marketplaceLimit
        )
}
