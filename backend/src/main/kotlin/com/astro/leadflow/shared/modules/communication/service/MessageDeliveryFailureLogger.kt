package com.astro.leadflow.shared.modules.communication.service

import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MessageDeliveryFailureLogger {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun logFailure(
        lead: Lead,
        organization: Organization,
        marketplaceType: MarketplaceType?,
        reason: String,
        error: Throwable
    ) {
        log.error(
            "message_delivery_failure orgId={} leadId={} marketplaceType={} reason={} error={}",
            organization.id,
            lead.id,
            marketplaceType,
            reason,
            error.message,
            error
        )
    }
}
