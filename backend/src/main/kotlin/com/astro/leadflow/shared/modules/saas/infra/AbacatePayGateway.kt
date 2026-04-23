package com.astro.leadflow.shared.modules.saas.infra

interface AbacatePayGateway {
    fun createProduct(request: AbacateCreateProductRequest): AbacateProductResponse

    fun createCustomer(request: AbacateCreateCustomerRequest): AbacateCustomerResponse

    fun createSubscriptionCheckout(request: AbacateCreateSubscriptionCheckoutRequest): AbacateCheckoutResponse

    fun cancelSubscription(id: String): AbacateSubscriptionResponse
}
