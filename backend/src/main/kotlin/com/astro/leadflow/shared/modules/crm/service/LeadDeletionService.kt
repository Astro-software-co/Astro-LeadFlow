package com.astro.leadflow.shared.modules.crm.service

import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.crm.domain.LeadDeletionLog
import com.astro.leadflow.shared.modules.crm.domain.LeadDeletionLogRepository
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.privacy.service.PrivacySubject
import com.astro.leadflow.shared.modules.privacy.service.SubjectHasher
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Exclusão manual e irreversível de lead (D13/T2.7): remove o card e tudo que depende dele; eventos de
 * marketplace ficam sem vínculo e sem payload. Grava auditoria sem PII. Anonimizar é a alternativa que
 * mantém as métricas.
 */
@Service
class LeadDeletionService(
    private val jdbc: JdbcTemplate,
    private val leadRepository: LeadRepository,
    private val deletionLogRepository: LeadDeletionLogRepository,
    private val hasher: SubjectHasher
) {

    @Transactional
    fun delete(organizationId: UUID, leadId: UUID, includeSamePhone: Boolean, deletedBy: UUID?, reason: String?): LeadDeletionResult {
        val lead = leadRepository.findByOrganizationIdAndId(organizationId, leadId)
            .orElseThrow { ResourceNotFoundException("Lead", leadId) }
        val ids = if (includeSamePhone) idsByPhone(organizationId, lead.phoneNumber) else listOf(lead.id)
        return deleteAll(organizationId, ids, PrivacySubject(lead.phoneNumber, isEmail = false), deletedBy, reason)
    }

    /** Usado pela solicitação de titular do tipo DELETION: exclui todos os leads do identificador. */
    @Transactional
    fun deleteBySubject(organizationId: UUID, subject: PrivacySubject, deletedBy: UUID?, reason: String?): LeadDeletionResult {
        val column = if (subject.isEmail) "lower(email)" else "phone_number"
        val ids = jdbc.queryForList(
            "SELECT id FROM leads WHERE organization_id = ? AND $column = ?", UUID::class.java, organizationId, subject.value
        )
        if (ids.isEmpty()) return LeadDeletionResult(0, 0)
        return deleteAll(organizationId, ids, subject, deletedBy, reason)
    }

    private fun idsByPhone(organizationId: UUID, phone: String): List<UUID> =
        jdbc.queryForList("SELECT id FROM leads WHERE organization_id = ? AND phone_number = ?", UUID::class.java, organizationId, phone)

    private fun deleteAll(organizationId: UUID, ids: List<UUID>, subject: PrivacySubject, deletedBy: UUID?, reason: String?): LeadDeletionResult {
        var messages = 0
        ids.forEach { id ->
            messages += jdbc.update("DELETE FROM chat_messages WHERE lead_id = ?", id)
            jdbc.update("DELETE FROM bot_session WHERE lead_id = ?", id)
            jdbc.update("DELETE FROM webmotors_lead_inbox WHERE lead_id = ?", id)
            jdbc.update("UPDATE marketplace_webhook_events SET raw_payload = '{}'::jsonb, error_message = NULL, lead_id = NULL WHERE lead_id = ?", id)
            jdbc.update("DELETE FROM lead_stage_history WHERE lead_id = ?", id)
            jdbc.update("DELETE FROM leads WHERE id = ? AND organization_id = ?", id, organizationId)
        }
        deletionLogRepository.save(
            LeadDeletionLog(
                organizationId = organizationId, deletedBy = deletedBy, subjectHash = hasher.hash(subject),
                cardsDeleted = ids.size, messagesDeleted = messages, reason = reason?.take(500)
            )
        )
        return LeadDeletionResult(ids.size, messages)
    }
}
