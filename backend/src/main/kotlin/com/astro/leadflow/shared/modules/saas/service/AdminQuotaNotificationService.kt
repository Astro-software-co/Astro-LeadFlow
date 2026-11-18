package com.astro.leadflow.shared.modules.saas.service

import com.astro.leadflow.shared.modules.iam.domain.Role
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AdminQuotaNotificationService(
    private val userRepository: UserRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun notifyMarketplaceLimitReached(
        organizationId: UUID,
        marketplaceType: MarketplaceType
    ) {
        val adminEmails = userRepository.findActiveByOrganizationAndRole(organizationId, Role.ADMIN)
            .map { it.email }

        log.warn(
            "Marketplace {} atingiu limite individual na organização {}. Admins notificados: {}",
            marketplaceType,
            organizationId,
            adminEmails.joinToString(", ")
        )
    }
}
