package com.astro.leadflow.shared.modules.privacy.job

import com.astro.leadflow.shared.modules.privacy.service.PrivacyRetentionService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Liga com `privacy.retention.enabled=true` (padrão em `prod`; desligado em dev). */
@Component
@ConditionalOnProperty(prefix = "privacy.retention", name = ["enabled"], havingValue = "true")
class PrivacyRetentionJob(private val retention: PrivacyRetentionService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${privacy.retention.cron:0 30 3 * * *}")
    fun run() {
        val report = retention.purge()
        log.info(
            "Retenção LGPD: payloads de marketplace={} propostas Webmotors={} erros apagados={}",
            report.eventPayloadsCleared, report.inboxPayloadsCleared, report.errorsDeleted
        )
    }
}
