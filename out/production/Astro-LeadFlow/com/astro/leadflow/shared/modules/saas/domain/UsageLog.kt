package com.astro.leadflow.modules.saas.domain

import com.astro.leadflow.modules.iam.domain.Organization
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "usage_logs",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["organization_id", "marketplace_name", "period_month", "period_year"]
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

    // Null = contabiliza no total geral da organização
    @Column(name = "marketplace_name")
    val marketplaceName: String? = null,

    @Column(name = "messages_sent", nullable = false)
    var messagesSent: Int = 0,

    @Column(name = "period_month", nullable = false)
    val periodMonth: Int,

    @Column(name = "period_year", nullable = false)
    val periodYear: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun increment() { messagesSent++ }
}