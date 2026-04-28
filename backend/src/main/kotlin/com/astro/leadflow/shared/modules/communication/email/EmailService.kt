package com.astro.leadflow.shared.modules.communication.email

interface EmailService {
    fun sendBotQuotaWarning(
        to: String,
        organizationName: String,
        usedTokens: Int,
        maxTokens: Int
    )
}
