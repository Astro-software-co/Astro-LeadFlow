package com.astro.leadflow.shared.modules.saas.api.requests

data class AbacatePayWebhookEvent(
    val id: String? = null,
    val event: String? = null,
    val apiVersion: Int? = null,
    val devMode: Boolean? = null,
    val data: AbacatePayWebhookData? = null,
)

data class AbacatePayWebhookData(
    val subscription: AbacatePayWebhookSubscription? = null,
    val payment: AbacatePayWebhookPayment? = null,
    val checkout: AbacatePayWebhookCheckout? = null,
    val customer: AbacatePayWebhookCustomer? = null,
)

data class AbacatePayWebhookSubscription(
    val id: String? = null,
    val amount: Long? = null,
    val status: String? = null,
    val frequency: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val canceledAt: String? = null,
)

data class AbacatePayWebhookPayment(
    val id: String? = null,
    val externalId: String? = null,
    val amount: Long? = null,
    val paidAmount: Long? = null,
    val status: String? = null,
    val receiptUrl: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class AbacatePayWebhookCheckout(
    val id: String? = null,
    val externalId: String? = null,
    val url: String? = null,
    val amount: Long? = null,
    val paidAmount: Long? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class AbacatePayWebhookCustomer(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val taxId: String? = null,
)
