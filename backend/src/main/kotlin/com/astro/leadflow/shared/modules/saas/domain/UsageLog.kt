package com.astro.leadflow.shared.modules.saas.domain

import com.astro.leadflow.shared.modules.iam.domain.Organization
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "usage_logs",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["organization_id", "marketplace_name", "period_month", "period_year", "metric_type"]
        )
    ]
)
class UsageLog(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @Column(name = "marketplace_name")
    val marketplaceName: String? = null,

    @Column(name = "messages_sent", nullable = false)
    var messagesSent: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    var metricType: UsageMetricType = UsageMetricType.MESSAGE_SENT,

    @Column(name = "period_month", nullable = false)
    val periodMonth: Int,

    @Column(name = "period_year", nullable = false)
    val periodYear: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun increment() { messagesSent++ }
}
