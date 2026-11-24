package com.astro.leadflow.shared.modules.privacy.service

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.crm.service.LeadDeletionService
import com.astro.leadflow.shared.modules.privacy.api.requests.CreateDataSubjectRequestRequest
import com.astro.leadflow.shared.modules.privacy.api.requests.ExecuteDataSubjectRequestRequest
import com.astro.leadflow.shared.modules.privacy.api.responses.DataSubjectRequestResponse
import com.astro.leadflow.shared.modules.privacy.config.PrivacyProperties
import com.astro.leadflow.shared.modules.privacy.domain.DataSubjectRequest
import com.astro.leadflow.shared.modules.privacy.domain.DataSubjectRequestRepository
import com.astro.leadflow.shared.modules.privacy.domain.DataSubjectRequestStatus
import com.astro.leadflow.shared.modules.privacy.domain.DataSubjectRequestType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/** Solicitações de titulares (LGPD art. 18): registro com prazo, execução e rejeição. */
@Service
class DataSubjectRequestService(
    private val repository: DataSubjectRequestRepository,
    private val hasher: SubjectHasher,
    private val anonymization: LeadAnonymizationService,
    private val deletion: LeadDeletionService,
    private val props: PrivacyProperties,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    @Transactional
    fun create(organizationId: UUID, req: CreateDataSubjectRequestRequest): DataSubjectRequestResponse {
        val subject = hasher.normalize(req.identifier!!)
        val received = req.receivedAt ?: now()
        val saved = repository.save(
            DataSubjectRequest(
                organizationId = organizationId, type = req.type!!, subjectHash = hasher.hash(subject),
                subjectHint = hasher.hint(subject), channel = req.channel?.trim()?.ifBlank { null },
                receivedAt = received, dueAt = received.plusDays(props.slaDays)
            )
        )
        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun list(organizationId: UUID, status: DataSubjectRequestStatus?): List<DataSubjectRequestResponse> =
        (status?.let { repository.findAllByOrganizationIdAndStatusOrderByReceivedAtDesc(organizationId, it) }
            ?: repository.findAllByOrganizationIdOrderByReceivedAtDesc(organizationId)).map(::toResponse)

    @Transactional
    fun execute(organizationId: UUID, id: UUID, req: ExecuteDataSubjectRequestRequest, userId: UUID?): DataSubjectRequestResponse {
        val request = openRequest(organizationId, id)
        val subject = hasher.normalize(req.identifier!!)
        if (hasher.hash(subject) != request.subjectHash)
            throw InvalidOperationException("O identificador informado não corresponde ao desta solicitação.")

        request.resultSummary = when (request.type) {
            DataSubjectRequestType.ANONYMIZATION -> anonymization.anonymize(organizationId, subject).let {
                if (it.isEmpty()) NO_DATA
                else "Leads anonimizados: ${it.leadsAnonymized}; mensagens: ${it.messagesRedacted}; sessões do bot: ${it.sessionsCleared}; " +
                    "eventos de marketplace: ${it.eventsCleared}; propostas Webmotors: ${it.inboxCleared}; registros de erro: ${it.errorsRedacted}."
            }
            DataSubjectRequestType.DELETION -> deletion.deleteBySubject(organizationId, subject, userId, "Solicitação de titular $id").let {
                if (it.cardsDeleted == 0) NO_DATA else "Cards excluídos: ${it.cardsDeleted}; mensagens: ${it.messagesDeleted}."
            }
            DataSubjectRequestType.ACCESS, DataSubjectRequestType.CORRECTION ->
                req.resultSummary?.trim()?.ifBlank { null }
                    ?: throw BusinessException("Descreva como a solicitação foi atendida (resultSummary).")
        }
        return complete(request, DataSubjectRequestStatus.COMPLETED, userId)
    }

    @Transactional
    fun reject(organizationId: UUID, id: UUID, reason: String, userId: UUID?): DataSubjectRequestResponse {
        val request = openRequest(organizationId, id)
        request.resultSummary = reason.trim()
        return complete(request, DataSubjectRequestStatus.REJECTED, userId)
    }

    private fun openRequest(organizationId: UUID, id: UUID): DataSubjectRequest {
        val request = repository.findByOrganizationIdAndId(organizationId, id)
            .orElseThrow { ResourceNotFoundException("Solicitação de titular", id) }
        if (!request.isOpen()) throw InvalidOperationException("A solicitação já foi encerrada (${request.status}).")
        return request
    }

    private fun complete(request: DataSubjectRequest, status: DataSubjectRequestStatus, userId: UUID?): DataSubjectRequestResponse {
        request.status = status
        request.completedAt = now()
        request.handledBy = userId
        return toResponse(repository.save(request))
    }

    private fun now() = LocalDateTime.now(clock)

    private fun toResponse(r: DataSubjectRequest) = DataSubjectRequestResponse(
        id = r.id, type = r.type, status = r.status, subjectHint = r.subjectHint, channel = r.channel,
        receivedAt = r.receivedAt, dueAt = r.dueAt, completedAt = r.completedAt,
        overdue = r.isOpen() && r.dueAt.isBefore(now()), resultSummary = r.resultSummary
    )

    companion object {
        const val NO_DATA = "Nenhum dado encontrado para o identificador."
    }
}
