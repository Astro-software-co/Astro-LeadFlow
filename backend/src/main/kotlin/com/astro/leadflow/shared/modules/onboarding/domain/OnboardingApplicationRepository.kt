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

    @Query(
        value = """
            SELECT a FROM OnboardingApplication a
            WHERE (:status IS NULL OR a.status = :status)
            AND (CAST(:query AS string) IS NULL
                 OR LOWER(a.companyName) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))
                 OR LOWER(a.ownerEmail)  LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')))
        """,
        countQuery = """
            SELECT COUNT(a) FROM OnboardingApplication a
            WHERE (:status IS NULL OR a.status = :status)
            AND (CAST(:query AS string) IS NULL
                 OR LOWER(a.companyName) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))
                 OR LOWER(a.ownerEmail)  LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')))
        """
    )
    fun search(
        status: OnboardingStatus?,
        query: String?,
        pageable: Pageable
    ): Page<OnboardingApplication>
}
