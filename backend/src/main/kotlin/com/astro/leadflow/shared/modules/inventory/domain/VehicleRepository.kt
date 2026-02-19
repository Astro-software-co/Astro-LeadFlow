package com.astro.leadflow.modules.inventory.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface VehicleRepository : JpaRepository<Vehicle, UUID> {

    fun findAllByOrganizationId(organizationId: UUID): List<Vehicle>

    fun findByOrganizationIdAndId(organizationId: UUID, id: UUID): Optional<Vehicle>

    fun findAllByOrganizationIdAndStatus(
        organizationId: UUID,
        status: VehicleStatus
    ): List<Vehicle>

    // Busca por marca ou modelo (usado na tela de estoque)
    @Query("""
        SELECT v FROM Vehicle v
        WHERE v.organization.id = :orgId
        AND (LOWER(v.brand) LIKE LOWER(CONCAT('%', :term, '%'))
        OR   LOWER(v.model) LIKE LOWER(CONCAT('%', :term, '%')))
    """)
    fun searchByBrandOrModel(
        @Param("orgId") orgId: UUID,
        @Param("term") term: String
    ): List<Vehicle>

    // Contagem por status para rodapé da tela de estoque
    @Query("""
        SELECT v.status, COUNT(v)
        FROM Vehicle v
        WHERE v.organization.id = :orgId
        GROUP BY v.status
    """)
    fun countByStatus(@Param("orgId") orgId: UUID): List<Array<Any>>


// Distribuição de vendidos vs em estoque por categoria
    @Query("""
        SELECT v.category,
            SUM(CASE WHEN v.status = 'SOLD' THEN 1 ELSE 0 END),
            SUM(CASE WHEN v.status != 'SOLD' THEN 1 ELSE 0 END)
        FROM Vehicle v
        WHERE v.organization.id = :orgId
        GROUP BY v.category
    """)
    
    fun countSoldAndStockByCategory(@Param("orgId") orgId: UUID): List<Array<Any>>

}