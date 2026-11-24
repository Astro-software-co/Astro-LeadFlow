package com.astro.leadflow.shared.modules.privacy.service

import com.astro.leadflow.shared.util.PiiMasker
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * Anonimiza todos os dados pessoais de um titular numa organização (LGPD art. 18, IV), mantendo o card e as
 * métricas. Idempotente. SQL direto porque toca tabelas de vários módulos numa única transação.
 */
@Service
class LeadAnonymizationService(
    private val jdbc: JdbcTemplate,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    @Transactional
    fun anonymize(organizationId: UUID, subject: PrivacySubject): AnonymizationReport {
        val column = if (subject.isEmail) "lower(email)" else "phone_number"
        val leadIds = jdbc.queryForList(
            "SELECT id FROM leads WHERE organization_id = ? AND anonymized_at IS NULL AND $column = ?",
            UUID::class.java, organizationId, subject.value
        )
        val now = LocalDateTime.now(clock)
        var messages = 0
        var sessions = 0
        var events = 0
        var inbox = 0
        leadIds.forEach { id ->
            jdbc.update(
                "UPDATE leads SET name = ?, phone_number = ?, email = NULL, notes = NULL, anonymized_at = ?, updated_at = ? WHERE id = ?",
                ANONYMIZED_NAME, "ANON-" + UUID.randomUUID().toString().take(8), now, now, id
            )
            messages += jdbc.update("UPDATE chat_messages SET content = ? WHERE lead_id = ? AND content <> ?", REDACTED, id, REDACTED)
            sessions += jdbc.update("UPDATE bot_session SET slot_values = '{}'::jsonb WHERE lead_id = ?", id)
            events += jdbc.update("UPDATE marketplace_webhook_events SET raw_payload = '{}'::jsonb, error_message = NULL WHERE lead_id = ?", id)
            inbox += jdbc.update("UPDATE webmotors_lead_inbox SET message = NULL, raw_payload = NULL WHERE lead_id = ?", id)
        }
        return AnonymizationReport(leadIds.size, messages, sessions, events, inbox, redactErrors(organizationId, subject))
    }

    /** Erros persistidos podem ter o identificador em texto livre (logs anteriores à máscara global). */
    private fun redactErrors(organizationId: UUID, subject: PrivacySubject): Int {
        val mask = if (subject.isEmail) PiiMasker.maskEmail(subject.value) else PiiMasker.maskPhone(subject.value)
        val variants = if (subject.isEmail) listOf(subject.value)
        else subject.value.removePrefix("+").let { digits -> listOf(subject.value, digits, digits.removePrefix("55")) }
        return variants.sumOf { v ->
            jdbc.update(
                """
                UPDATE application_error
                   SET message = replace(message, ?, ?), stack_trace = replace(stack_trace, ?, ?)
                 WHERE (organization_id = ? OR organization_id IS NULL)
                   AND (strpos(message, ?) > 0 OR strpos(coalesce(stack_trace, ''), ?) > 0)
                """.trimIndent(),
                v, mask, v, mask, organizationId, v, v
            )
        }
    }

    companion object {
        const val ANONYMIZED_NAME = "Titular anonimizado"
        const val REDACTED = "[mensagem removida]"
    }
}
