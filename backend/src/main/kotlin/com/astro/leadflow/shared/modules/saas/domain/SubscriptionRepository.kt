package com.astro.leadflow.shared.modules.saas.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface SubscriptionRepository : JpaRepository<Subscription, UUID> {
    fun findByOrganizationId(organizationId: UUID): Optional<Subscription>
    fun findByAbacateSubscriptionId(abacateSubscriptionId: String): Optional<Subscription>
}
