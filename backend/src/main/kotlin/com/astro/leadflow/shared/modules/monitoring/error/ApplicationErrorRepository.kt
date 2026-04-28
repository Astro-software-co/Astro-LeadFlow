package com.astro.leadflow.shared.modules.monitoring.error

import com.astro.leadflow.shared.modules.monitoring.domain.entity.ApplicationError
import com.astro.leadflow.shared.modules.monitoring.domain.enums.LogLevel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface ApplicationErrorRepository : JpaRepository<ApplicationError, Long> {
    @Query(
        """
        SELECT e FROM ApplicationError e
        WHERE (:level IS NULL OR e.level = :level)
        AND (:from IS NULL OR e.occurredAt >= :from)
        AND (:to IS NULL OR e.occurredAt < :to)
        AND (:service IS NULL OR LOWER(e.loggerName) LIKE LOWER(CONCAT('%', CAST(:service AS string), '%')))
        AND (:correlationId IS NULL OR e.correlationId = :correlationId)
        ORDER BY e.occurredAt DESC
        """
    )
    fun search(
        @Param("level") level: LogLevel?,
        @Param("from") from: OffsetDateTime?,
        @Param("to") to: OffsetDateTime?,
        @Param("service") service: String?,
        @Param("correlationId") correlationId: String?,
        pageable: Pageable
    ): Page<ApplicationError>

    fun countByOccurredAtAfter(occurredAt: OffsetDateTime): Long
}
