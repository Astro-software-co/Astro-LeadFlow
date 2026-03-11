package com.astro.leadflow.shared.modules.iam.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean
    fun findAllByOrganizationId(organizationId: UUID): List<User>

    @Query("""
        SELECT u FROM User u 
        WHERE u.organization.id = :orgId 
        AND u.role = :role 
        AND u.status = 'ACTIVE'
    """)
    fun findActiveByOrganizationAndRole(orgId: UUID, role: Role): List<User>
}