package com.astro.leadflow.shared.modules.integrations.webmotors.service

import com.astro.leadflow.shared.modules.integrations.webmotors.config.WebmotorsProperties
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.AcceptMode
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.InboxStatus
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApi
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Um ciclo de sincronização de uma conta: lê as propostas pendentes, grava as novas na caixa de
 * entrada, aceita automaticamente (se o modo for AUTO) e retenta importações pendentes.
 * Não é transacional: as chamadas HTTP ficam fora de transação de banco.
 */
@Service
class WebmotorsLeadSyncService(
    private val api: WebmotorsApi,
    private val accounts: WebmotorsAccountService,
    private val inbox: WebmotorsInboxService,
    private val decisions: WebmotorsDecisionService,
    private val importer: WebmotorsLeadImporter,
    private val props: WebmotorsProperties,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun syncAccount(accountId: UUID): SyncReport {
        val runtime = accounts.runtimeOf(accountId)
        var fetched = 0
        var created = 0
        var skipped = 0

        try {
            var page: Int? = null
            var pagesRead = 0
            do {
                val result = api.fetchPendingLeads(runtime.credentials, page)
                pagesRead++

                for (item in result.mensagens) {
                    fetched++
                    val lojaId = item.loja?.id
                    if (item.id == null || (runtime.storeId != null && lojaId != null && lojaId != runtime.storeId)) {
                        skipped++
                        continue
                    }
                    val (_, isNew) = inbox.upsert(accountId, item, objectMapper.writeValueAsString(item))
                    if (isNew) created++
                }

                val total = result.totalPaginas ?: 1
                val current = result.paginaAtual ?: pagesRead
                page = if (current < total && pagesRead < props.maxPagesPerSync) current + 1 else null
            } while (page != null)

            val accepted =
                if (runtime.acceptMode == AcceptMode.AUTO) decisions.acceptAllPending(accountId) else 0

            var retried = 0
            for (id in inbox.idsByStatus(accountId, InboxStatus.ACCEPTED, InboxStatus.AWAITING_CONTACT)) {
                try {
                    importer.tryImport(id)
                    retried++
                } catch (e: Exception) {
                    log.warn("Falha ao importar proposta Webmotors {}: {}", id, e.message)
                }
            }

            accounts.recordSync(accountId, true, null)
            return SyncReport(fetched, created, accepted, retried, skipped)
        } catch (e: Exception) {
            log.warn("Sync Webmotors da conta {} falhou: {}", accountId, e.message)
            accounts.recordSync(accountId, false, e.message ?: e.javaClass.simpleName)
            return SyncReport(fetched, created, skipped = skipped, error = e.message ?: e.javaClass.simpleName)
        }
    }
}
