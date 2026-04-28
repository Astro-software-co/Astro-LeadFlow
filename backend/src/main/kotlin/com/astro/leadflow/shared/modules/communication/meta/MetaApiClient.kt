package com.astro.leadflow.shared.modules.communication.meta

interface MetaApiClient {
    fun sendText(
        phoneNumberId: String,
        accessToken: String,
        toPhone: String,
        message: String
    ): String

    fun sendTemplate(
        phoneNumberId: String,
        accessToken: String,
        toPhone: String,
        templateName: String,
        languageCode: String = "pt_BR",
        components: List<Map<String, Any>> = emptyList()
    ): String
}
