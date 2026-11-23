package com.astro.leadflow.shared.modules.integrations.webmotors.service

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.ExternalServiceException
import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.InboxStatus
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApi
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApiException
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmKeyValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

/** Aceitar/recusar propostas na Webmotors e refletir o resultado na caixa de entrada. */
@Service
class WebmotorsDecisionService(
    private val api: WebmotorsApi,
    private val inbox: WebmotorsInboxService,
    private val accounts: WebmotorsAccountService,
    private val importer: WebmotorsLeadImporter
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Aceita na Webmotors e já tenta criar o Lead. */
    fun accept(organizationId: UUID, entryId: UUID): InboxEntry =
        accept(inbox.getForOrganization(organizationId, entryId))

    fun refuse(organizationId: UUID, entryId: UUID, reasonCode: Int, message: String?): InboxEntry {
        val entry = inbox.getForOrganization(organizationId, entryId)
        requirePending(entry)
        val runtime = accounts.runtimeOf(entry.accountId)
        try {
            api.refuse(runtime.credentials, entry.externalId, entry.leadType, reasonCode, message)
        } catch (e: WebmotorsApiException) {
            throw ExternalServiceException("Webmotors", e.message ?: "falha ao recusar")
        }
        return inbox.markRefused(entry.id, reasonCode, message)
    }

    /** Modo AUTO: aceita tudo que está pendente. Falhas isoladas não interrompem as demais. */
    fun acceptAllPending(accountId: UUID): Int {
        var accepted = 0
        for (id in inbox.idsByStatus(accountId, InboxStatus.PENDING)) {
            try {
                accept(inbox.get(id))
                accepted++
            } catch (e: Exception) {
                log.warn("Falha ao aceitar proposta Webmotors {}: {}", id, e.message)
            }
        }
        return accepted
    }

    fun refusalReasons(organizationId: UUID, leadType: Int): List<WmKeyValue> {
        val runtime = accounts.runtimeOfOrganization(organizationId)
        return try {
            api.refusalReasons(runtime.credentials, leadType)
        } catch (e: WebmotorsApiException) {
            throw ExternalServiceException("Webmotors", e.message ?: "falha ao obter motivos")
        }
    }

    private fun accept(entry: InboxEntry): InboxEntry {
        requirePending(entry)
        val runtime = accounts.runtimeOf(entry.accountId)
        try {
            api.accept(runtime.credentials, entry.externalId, entry.leadType)
        } catch (e: WebmotorsApiException) {
            throw ExternalServiceException("Webmotors", e.message ?: "falha ao aceitar")
        }
        inbox.markAccepted(entry.id)
        // Aceitar já é definitivo na Webmotors; um erro ao importar não pode desfazer isso.
        return try {
            importer.tryImport(entry.id)
        } catch (e: BusinessException) {
            log.warn("Proposta {} aceita, mas importação falhou: {}", entry.id, e.message)
            inbox.get(entry.id)
        }
    }

    private fun requirePending(entry: InboxEntry) {
        if (entry.status != InboxStatus.PENDING)
            throw InvalidOperationException("A proposta já foi tratada (status ${entry.status}).")
    }
}
