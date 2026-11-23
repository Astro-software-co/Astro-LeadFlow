package com.astro.leadflow.shared.modules.integrations.webmotors.service

import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.InboxStatus
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.WebmotorsAccountRepository
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.WebmotorsLeadInbox
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.WebmotorsLeadInboxRepository
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmLeadItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

/** Operações de banco da caixa de entrada. Cada método é uma transação curta. */
@Service
class WebmotorsInboxService(
    private val inboxRepository: WebmotorsLeadInboxRepository,
    private val accountRepository: WebmotorsAccountRepository
) {

    /** Insere a proposta ou, se já existir e ainda estiver pendente, atualiza os dados remotos. */
    @Transactional
    fun upsert(accountId: UUID, item: WmLeadItem, rawJson: String?): Pair<InboxEntry, Boolean> {
        val account = accountRepository.findById(accountId)
            .orElseThrow { ResourceNotFoundException("Conta Webmotors", accountId) }
        val externalId = requireNotNull(item.id) { "Lead Webmotors sem id." }
        val type = item.tipoLead ?: 0

        val existing = inboxRepository
            .findByOrganizationIdAndExternalIdAndLeadType(account.organization.id, externalId, type)
            .orElse(null)

        val row = existing ?: WebmotorsLeadInbox(
            organization = account.organization, account = account, externalId = externalId, leadType = type
        )
        if (existing == null || existing.status == InboxStatus.PENDING) {
            row.message = item.mensagem
            row.channel = item.midia
            row.typeDescription = item.tipoLeadDesc
            row.remoteStatus = item.status
            row.remoteCreatedAt = item.dataMensagem
            row.adId = item.anuncio?.id
            row.adBrand = item.anuncio?.marca
            row.adModel = item.anuncio?.modelo
            row.adVersion = item.anuncio?.versao
            row.adYear = item.anuncio?.anoModelo ?: item.anuncio?.anoFabricacao
            row.adPlate = item.anuncio?.placa
            row.adPrice = item.anuncio?.valor
            row.rawPayload = rawJson
        }
        return inboxRepository.save(row).toEntry() to (existing == null)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): InboxEntry = load(id).toEntry()

    @Transactional(readOnly = true)
    fun getForOrganization(organizationId: UUID, id: UUID): InboxEntry =
        inboxRepository.findByOrganizationIdAndId(organizationId, id)
            .orElseThrow { ResourceNotFoundException("Proposta Webmotors", id) }
            .toEntry()

    @Transactional(readOnly = true)
    fun list(organizationId: UUID, status: InboxStatus?): List<InboxEntry> =
        (if (status == null) inboxRepository.findAllByOrganizationIdOrderByReceivedAtDesc(organizationId)
        else inboxRepository.findAllByOrganizationIdAndStatusOrderByReceivedAtDesc(organizationId, status))
            .map { it.toEntry() }

    @Transactional(readOnly = true)
    fun idsByStatus(accountId: UUID, vararg statuses: InboxStatus): List<UUID> =
        inboxRepository.findAllByAccountIdAndStatusIn(accountId, statuses.toList()).map { it.id }

    @Transactional
    fun markAccepted(id: UUID) = update(id) {
        status = InboxStatus.ACCEPTED
        decidedAt = LocalDateTime.now()
        lastError = null
    }

    @Transactional
    fun markRefused(id: UUID, code: Int, message: String?) = update(id) {
        status = InboxStatus.REFUSED
        refusalCode = code
        refusalMessage = message?.take(500)
        decidedAt = LocalDateTime.now()
    }

    @Transactional
    fun markImported(id: UUID, leadId: UUID) = update(id) {
        status = InboxStatus.IMPORTED
        this.leadId = leadId
        lastError = null
    }

    /** Falha ao obter/usar o contato: conta a tentativa e vira FAILED ao esgotar [maxAttempts]. */
    @Transactional
    fun markContactIssue(id: UUID, error: String, maxAttempts: Int): InboxEntry = update(id) {
        attempts += 1
        lastError = error.take(500)
        status = if (attempts >= maxAttempts) InboxStatus.FAILED else InboxStatus.AWAITING_CONTACT
    }

    private fun update(id: UUID, block: WebmotorsLeadInbox.() -> Unit): InboxEntry {
        val row = load(id)
        row.block()
        return inboxRepository.save(row).toEntry()
    }

    private fun load(id: UUID) = inboxRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Proposta Webmotors", id) }

    private fun WebmotorsLeadInbox.toEntry() = InboxEntry(
        id = id, organizationId = organization.id, accountId = account.id,
        externalId = externalId, leadType = leadType, status = status, message = message,
        channel = channel, typeDescription = typeDescription, remoteStatus = remoteStatus,
        remoteCreatedAt = remoteCreatedAt, adId = adId, adBrand = adBrand, adModel = adModel,
        adVersion = adVersion, adYear = adYear, adPlate = adPlate, adPrice = adPrice,
        refusalCode = refusalCode, refusalMessage = refusalMessage, leadId = leadId,
        attempts = attempts, lastError = lastError, receivedAt = receivedAt, decidedAt = decidedAt
    )
}
