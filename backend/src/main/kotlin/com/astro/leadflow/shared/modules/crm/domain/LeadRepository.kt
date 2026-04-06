package com.astro.leadflow.shared.modules.crm.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

interface LeadRepository : JpaRepository<Lead, UUID> {

    fun findByOrganizationIdAndPhoneNumber(
    organizationId: UUID,
    phoneNumber: String
    ): Optional<Lead>

    fun findAllByOrganizationId(organizationId: UUID): List<Lead>

    fun findAllByOrganizationIdAndPipelineStage(
        organizationId: UUID,
        stage: PipelineStage
    ): List<Lead>

    fun findByOrganizationIdAndId(organizationId: UUID, id: UUID): Optional<Lead>

    // Verifica duplicidade por telefone dentro da organização
    fun existsByOrganizationIdAndPhoneNumber(
        organizationId: UUID,
        phoneNumber: String
    ): Boolean

    // Kanban board completo agrupado por stage
    @Query("""
        SELECT l FROM Lead l
        LEFT JOIN FETCH l.assignedTo
        LEFT JOIN FETCH l.vehicle
        WHERE l.organization.id = :orgId
        AND l.pipelineStage != 'ARCHIVED'
        ORDER BY l.pipelineStage, l.createdAt DESC
    """)
    fun findBoardByOrganization(@Param("orgId") orgId: UUID): List<Lead>

    @Query(
        value = """
            SELECT l FROM Lead l
            LEFT JOIN FETCH l.assignedTo
            LEFT JOIN FETCH l.vehicle
            WHERE l.organization.id = :orgId
            AND (
              :search IS NULL
              OR LOWER(COALESCE(l.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(l.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(COALESCE(l.email, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (
              :origin IS NULL
              OR LOWER(REPLACE(l.origin, '_', ' ')) = LOWER(REPLACE(:origin, '_', ' '))
            )
            AND (:stage IS NULL OR l.pipelineStage = :stage)
            AND (:assignedTo IS NULL OR l.assignedTo.id = :assignedTo)
            AND (:createdFrom IS NULL OR l.createdAt >= :createdFrom)
            AND (:createdTo IS NULL OR l.createdAt < :createdTo)
        """,
        countQuery = """
            SELECT COUNT(l) FROM Lead l
            WHERE l.organization.id = :orgId
            AND (
              :search IS NULL
              OR LOWER(COALESCE(l.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(l.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(COALESCE(l.email, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (
              :origin IS NULL
              OR LOWER(REPLACE(l.origin, '_', ' ')) = LOWER(REPLACE(:origin, '_', ' '))
            )
            AND (:stage IS NULL OR l.pipelineStage = :stage)
            AND (:assignedTo IS NULL OR l.assignedTo.id = :assignedTo)
            AND (:createdFrom IS NULL OR l.createdAt >= :createdFrom)
            AND (:createdTo IS NULL OR l.createdAt < :createdTo)
        """
    )
    fun findPageByOrganization(
        @Param("orgId") orgId: UUID,
        @Param("search") search: String?,
        @Param("origin") origin: String?,
        @Param("stage") stage: PipelineStage?,
        @Param("assignedTo") assignedTo: UUID?,
        @Param("createdFrom") createdFrom: LocalDateTime?,
        @Param("createdTo") createdTo: LocalDateTime?,
        pageable: Pageable
    ): Page<Lead>

    // Leads de um vendedor específico
    @Query("""
        SELECT l FROM Lead l
        WHERE l.organization.id = :orgId
        AND l.assignedTo.id = :sellerId
        AND l.pipelineStage != 'ARCHIVED'
    """)
    fun findByOrganizationAndSeller(
        @Param("orgId") orgId: UUID,
        @Param("sellerId") sellerId: UUID
    ): List<Lead>

    // Contagem por stage para dashboard
    @Query("""
        SELECT l.pipelineStage, COUNT(l)
        FROM Lead l
        WHERE l.organization.id = :orgId
        AND l.pipelineStage != 'ARCHIVED'
        GROUP BY l.pipelineStage
    """)

    fun countByStage(@Param("orgId") orgId: UUID): List<Array<Any>>

    // Contagem total de leads da organização
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.organization.id = :orgId")
    fun countByOrganizationId(@Param("orgId") orgId: UUID): Long

    // Contagem de leads por mês/ano
    @Query("""
        SELECT COUNT(l) FROM Lead l
        WHERE l.organization.id = :orgId
        AND MONTH(l.createdAt) = :month
        AND YEAR(l.createdAt) = :year
    """)
    fun countByOrganizationIdAndPeriod(
        @Param("orgId") orgId: UUID,
        @Param("month") month: Int,
        @Param("year") year: Int
    ): Long

    // Contagem de leads por estágio
    @Query("""
        SELECT COUNT(l) FROM Lead l
        WHERE l.organization.id = :orgId
        AND l.pipelineStage = :stage
    """)
    fun countByOrganizationIdAndStage(
        @Param("orgId") orgId: UUID,
        @Param("stage") stage: PipelineStage
    ): Long

    // Contagem de leads por estágio e período
    @Query("""
        SELECT COUNT(l) FROM Lead l
        WHERE l.organization.id = :orgId
        AND l.pipelineStage = :stage
        AND MONTH(l.createdAt) = :month
        AND YEAR(l.createdAt) = :year
    """)
    fun countByOrganizationIdAndStageAndPeriod(
        @Param("orgId") orgId: UUID,
        @Param("stage") stage: PipelineStage,
        @Param("month") month: Int,
        @Param("year") year: Int
    ): Long

    // Vendas concluídas por período (leads em COMPLETED com vehicle vinculado)
    @Query("""
        SELECT COUNT(l) FROM Lead l
        WHERE l.organization.id = :orgId
        AND l.pipelineStage = 'COMPLETED'
        AND l.vehicle IS NOT NULL
        AND MONTH(l.updatedAt) = :month
        AND YEAR(l.updatedAt) = :year
    """)
    fun countSalesByOrganizationAndPeriod(
        @Param("orgId") orgId: UUID,
        @Param("month") month: Int,
        @Param("year") year: Int
    ): Long

    // Soma de receita por período (salePrice dos veículos vendidos)
    @Query("""
        SELECT SUM(l.vehicle.salePrice)
        FROM Lead l
        WHERE l.organization.id = :orgId
        AND l.pipelineStage = 'COMPLETED'
        AND l.vehicle IS NOT NULL
        AND MONTH(l.updatedAt) = :month
        AND YEAR(l.updatedAt) = :year
    """)
    fun sumRevenueByOrganizationAndPeriod(
        @Param("orgId") orgId: UUID,
        @Param("month") month: Int,
        @Param("year") year: Int
    ): Double?
}
