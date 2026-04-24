package com.astro.leadflow.shared.modules.onboarding.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface OnboardingApplicationRepository : JpaRepository<OnboardingApplication, UUID> {

    fun findAllByStatus(status: OnboardingStatus, pageable: Pageable): Page<OnboardingApplication>

    fun existsByOwnerEmail(ownerEmail: String): Boolean

    fun existsByCnpj(cnpj: String): Boolean

    @Query("""
        SELECT a FROM OnboardingApplication a
        WHERE (:status IS NULL OR a.status = :status)
        AND (:query IS NULL OR LOWER(a.companyName) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(a.ownerEmail) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY a.createdAt DESC
    """)
    fun search(
        status: OnboardingStatus?,
        query: String?,
        pageable: Pageable
    ): Page<OnboardingApplication>
}
