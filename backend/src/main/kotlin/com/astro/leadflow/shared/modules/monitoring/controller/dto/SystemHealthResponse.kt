package com.astro.leadflow.shared.modules.monitoring.controller.dto

data class SystemHealthResponse(
    val abacatePayConfigured: Boolean,
    val metaWebhookSecretConfigured: Boolean,
    val metaApiTokenConfigured: Boolean,
    val redisUp: Boolean,
    val dbUp: Boolean,
    val chatbotEngineMode: String,
    val awsLexConfigured: Boolean,
    val awsBedrockConfigured: Boolean,
    val publicApiUrlConfigured: Boolean
)
