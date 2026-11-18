package com.astro.leadflow.shared.modules.saas.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface BillingHistoryEntryRepository : JpaRepository<BillingHistoryEntry, UUID> {
    fun findAllBySubscriptionIdOrderByBilledAtDescCreatedAtDesc(subscriptionId: UUID): List<BillingHistoryEntry>
    fun findByProviderEventId(providerEventId: String): Optional<BillingHistoryEntry>
}
