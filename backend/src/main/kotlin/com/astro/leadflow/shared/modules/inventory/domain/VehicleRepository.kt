package com.astro.leadflow.shared.modules.inventory.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

interface VehicleRepository : JpaRepository<Vehicle, UUID> {

    fun findAllByOrganizationId(organizationId: UUID): List<Vehicle>

    fun findByOrganizationIdAndId(organizationId: UUID, id: UUID): Optional<Vehicle>

    fun findAllByOrganizationIdAndStatus(
        organizationId: UUID,
        status: VehicleStatus
    ): List<Vehicle>

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

    @Query(
        value = """
            SELECT v FROM Vehicle v
            WHERE v.organization.id = :orgId
            AND (
              CAST(:search AS string) IS NULL
              OR LOWER(v.brand) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
              OR LOWER(v.model) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
            )
            AND (:status IS NULL OR v.status = :status)
            AND (:category IS NULL OR v.category = :category)
            AND (:year IS NULL OR v.year = :year)
            AND (:minPrice IS NULL OR v.salePrice >= :minPrice)
            AND (:maxPrice IS NULL OR v.salePrice <= :maxPrice)
        """,
        countQuery = """
            SELECT COUNT(v) FROM Vehicle v
            WHERE v.organization.id = :orgId
            AND (
              CAST(:search AS string) IS NULL
              OR LOWER(v.brand) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
              OR LOWER(v.model) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
            )
            AND (:status IS NULL OR v.status = :status)
            AND (:category IS NULL OR v.category = :category)
            AND (:year IS NULL OR v.year = :year)
            AND (:minPrice IS NULL OR v.salePrice >= :minPrice)
            AND (:maxPrice IS NULL OR v.salePrice <= :maxPrice)
        """
    )
    fun findPageByOrganization(
        @Param("orgId") orgId: UUID,
        @Param("search") search: String?,
        @Param("status") status: VehicleStatus?,
        @Param("category") category: VehicleCategory?,
        @Param("year") year: Int?,
        @Param("minPrice") minPrice: BigDecimal?,
        @Param("maxPrice") maxPrice: BigDecimal?,
        pageable: Pageable
    ): Page<Vehicle>

    @Query("""
        SELECT v.status, COUNT(v)
        FROM Vehicle v
        WHERE v.organization.id = :orgId
        GROUP BY v.status
    """)
    fun countByStatus(@Param("orgId") orgId: UUID): List<Array<Any>>

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
