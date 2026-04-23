package com.astro.leadflow.shared.modules.saas.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "billing_history_entries")
class BillingHistoryEntry(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    val subscription: Subscription,

    @Column(name = "provider_event_id", unique = true)
    var providerEventId: String? = null,

    @Column(name = "provider_payment_id")
    var providerPaymentId: String? = null,

    @Column(name = "provider_checkout_id")
    var providerCheckoutId: String? = null,

    @Column(name = "provider_subscription_id")
    var providerSubscriptionId: String? = null,

    @Column(name = "external_reference")
    var externalReference: String? = null,

    @Column(nullable = false)
    var description: String,

    @Column(name = "amount_brl", nullable = false)
    var amountBrl: BigDecimal,

    @Column(nullable = false)
    var status: String,

    @Column(name = "billed_at", nullable = false)
    var billedAt: LocalDate,

    @Column(name = "issued_at", nullable = false)
    var issuedAt: LocalDate,

    @Column(name = "invoice_url")
    var invoiceUrl: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
