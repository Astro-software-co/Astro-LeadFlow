package com.astro.leadflow.shared.modules.integrations.webmotors.service

import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.modules.integrations.webmotors.config.WebmotorsProperties
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.WebmotorsAccountRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

/**
 * Garante que uma conta só seja sincronizada por uma instância por vez (lease no banco)
 * e expõe os pontos de entrada: ciclo agendado e sincronização manual.
 */
@Service
class WebmotorsSyncCoordinator(
    private val accountRepository: WebmotorsAccountRepository,
    private val accounts: WebmotorsAccountService,
    private val syncService: WebmotorsLeadSyncService,
    private val props: WebmotorsProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Ciclo agendado: todas as contas habilitadas. Contas com lease ativo são puladas. */
    fun syncAll(): Int {
        var ran = 0
        for (id in accounts.enabledAccountIds()) {
            if (runWithLease(id) != null) ran++
        }
        return ran
    }

    /** Sincronização manual da organização. Falha se já houver uma em andamento. */
    fun syncOrganization(organizationId: UUID): SyncReport {
        val accountId = accounts.runtimeOfOrganization(organizationId).id
        return runWithLease(accountId)
            ?: throw InvalidOperationException("Já existe uma sincronização Webmotors em andamento.")
    }

    private fun runWithLease(accountId: UUID): SyncReport? {
        val now = LocalDateTime.now()
        if (accountRepository.claimLease(accountId, now, now.plusSeconds(props.leaseSeconds)) == 0) {
            log.debug("Conta Webmotors {} já está em sincronização; pulando.", accountId)
            return null
        }
        try {
            return syncService.syncAccount(accountId)
        } finally {
            accountRepository.releaseLease(accountId)
        }
    }
}
