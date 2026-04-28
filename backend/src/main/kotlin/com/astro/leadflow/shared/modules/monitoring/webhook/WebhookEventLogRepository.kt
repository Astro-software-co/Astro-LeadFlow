package com.astro.leadflow.shared.modules.monitoring.webhook

import com.astro.leadflow.shared.modules.monitoring.domain.entity.WebhookEventLog
import com.astro.leadflow.shared.modules.monitoring.domain.enums.WebhookOutcome
import com.astro.leadflow.shared.modules.monitoring.domain.enums.WebhookSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface WebhookEventLogRepository : JpaRepository<WebhookEventLog, Long> {
    @Query(
        """
        SELECT w FROM WebhookEventLog w
        WHERE (:source IS NULL OR w.source = :source)
        AND (:outcome IS NULL OR w.outcome = :outcome)
        AND (:from IS NULL OR w.receivedAt >= :from)
        AND (:to IS NULL OR w.receivedAt < :to)
        ORDER BY w.receivedAt DESC
        """
    )
    fun search(
        @Param("source") source: WebhookSource?,
        @Param("outcome") outcome: WebhookOutcome?,
        @Param("from") from: OffsetDateTime?,
        @Param("to") to: OffsetDateTime?,
        pageable: Pageable
    ): Page<WebhookEventLog>

    @Query(
        """
        SELECT w.source, w.outcome, COUNT(w)
        FROM WebhookEventLog w
        WHERE (:from IS NULL OR w.receivedAt >= :from)
        AND (:to IS NULL OR w.receivedAt < :to)
        GROUP BY w.source, w.outcome
        """
    )
    fun summarize(@Param("from") from: OffsetDateTime?, @Param("to") to: OffsetDateTime?): List<Array<Any>>
}
