package com.astro.leadflow.shared.modules.privacy.service

import com.astro.leadflow.shared.modules.monitoring.config.MonitoringConfig
import com.astro.leadflow.shared.modules.privacy.config.PrivacyProperties
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.time.OffsetDateTime

/**
 * Retenção (T2.5): payloads brutos de marketplace e da caixa Webmotors após `privacy.retention.webhook-payload-days`
 * e erros persistidos após `app.monitoring.error-retention-days` (propriedade que antes não era aplicada).
 */
@Service
class PrivacyRetentionService(
    private val jdbc: JdbcTemplate,
    private val props: PrivacyProperties,
    private val monitoring: MonitoringConfig,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    @Transactional
    fun purge(): RetentionReport {
        val payloadCutoff = LocalDateTime.now(clock).minusDays(props.retention.webhookPayloadDays)
        val errorCutoff = OffsetDateTime.now(clock).minusDays(monitoring.errorRetentionDays)
        return RetentionReport(
            eventPayloadsCleared = jdbc.update(
                "UPDATE marketplace_webhook_events SET raw_payload = '{}'::jsonb WHERE received_at < ? AND raw_payload::text <> '{}'",
                payloadCutoff
            ),
            inboxPayloadsCleared = jdbc.update(
                "UPDATE webmotors_lead_inbox SET raw_payload = NULL WHERE received_at < ? AND raw_payload IS NOT NULL",
                payloadCutoff
            ),
            errorsDeleted = jdbc.update("DELETE FROM application_error WHERE occurred_at < ?", errorCutoff)
        )
    }
}
