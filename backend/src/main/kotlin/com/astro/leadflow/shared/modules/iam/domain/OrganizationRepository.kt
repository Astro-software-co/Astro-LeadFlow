package com.astro.leadflow.shared.modules.iam.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface OrganizationRepository : JpaRepository<Organization, UUID> {
    fun findBySlug(slug: String): Optional<Organization>
    fun existsBySlug(slug: String): Boolean
    fun existsByName(name: String): Boolean
    fun findByWhatsappPhoneNumberId(phoneNumberId: String): Organization?
    fun findByApiKey(apiKey: String): Organization?
    fun findByReferralCode(referralCode: String): Optional<Organization>
}