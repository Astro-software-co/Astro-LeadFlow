package com.astro.leadflow.shared.modules.privacy.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "privacy")
data class PrivacyProperties(
    /** Segredo do HMAC dos identificadores de titular. Obrigatório em `prod` (fail-fast na phase G8). */
    val hashSecret: String = "",
    /** Prazo (dias corridos) para atender uma solicitação de titular. */
    val slaDays: Long = 15,
    val retention: Retention = Retention()
) {
    data class Retention(
        /** Liga o job diário de retenção (desligado em dev, ligado em `prod`). */
        val enabled: Boolean = false,
        val webhookPayloadDays: Long = 90,
        val cron: String = "0 30 3 * * *"
    )
}
