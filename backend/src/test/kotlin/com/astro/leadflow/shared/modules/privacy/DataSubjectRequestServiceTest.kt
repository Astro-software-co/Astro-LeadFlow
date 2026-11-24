package com.astro.leadflow.shared.modules.privacy

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.modules.crm.service.LeadDeletionResult
import com.astro.leadflow.shared.modules.crm.service.LeadDeletionService
import com.astro.leadflow.shared.modules.ingestion.service.NormalizationService
import com.astro.leadflow.shared.modules.privacy.api.requests.CreateDataSubjectRequestRequest
import com.astro.leadflow.shared.modules.privacy.api.requests.ExecuteDataSubjectRequestRequest
import com.astro.leadflow.shared.modules.privacy.config.PrivacyProperties
import com.astro.leadflow.shared.modules.privacy.domain.DataSubjectRequest
import com.astro.leadflow.shared.modules.privacy.domain.DataSubjectRequestRepository
import com.astro.leadflow.shared.modules.privacy.domain.DataSubjectRequestStatus
import com.astro.leadflow.shared.modules.privacy.domain.DataSubjectRequestType
import com.astro.leadflow.shared.modules.privacy.service.AnonymizationReport
import com.astro.leadflow.shared.modules.privacy.service.DataSubjectRequestService
import com.astro.leadflow.shared.modules.privacy.service.LeadAnonymizationService
import com.astro.leadflow.shared.modules.privacy.service.SubjectHasher
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class DataSubjectRequestServiceTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC)
    private val repo = mockk<DataSubjectRequestRepository>()
    private val hasher = SubjectHasher(PrivacyProperties(hashSecret = "s"), NormalizationService())
    private val anonymization = mockk<LeadAnonymizationService>()
    private val deletion = mockk<LeadDeletionService>()
    private val service = DataSubjectRequestService(repo, hasher, anonymization, deletion, PrivacyProperties(hashSecret = "s"), clock)
    private val org = UUID.randomUUID()
    private val saved = slot<DataSubjectRequest>()

    @BeforeEach
    fun setup() {
        every { repo.save(capture(saved)) } answers { saved.captured }
    }

    private fun created(type: DataSubjectRequestType = DataSubjectRequestType.ANONYMIZATION, receivedAt: LocalDateTime? = null): DataSubjectRequest {
        service.create(org, CreateDataSubjectRequestRequest(type, "(11) 98765-4321", "EMAIL", receivedAt))
        return saved.captured.also { every { repo.findByOrganizationIdAndId(org, it.id) } returns Optional.of(it) }
    }

    @Test
    fun `registro guarda hash e dica, nunca o telefone, com prazo de 15 dias`() {
        val r = created()

        assertEquals(64, r.subjectHash.length)
        assertEquals("+55******4321", r.subjectHint)
        assertFalse(r.subjectHash.contains("98765"))
        assertEquals(LocalDateTime.now(clock).plusDays(15), r.dueAt)
        assertEquals(DataSubjectRequestStatus.RECEIVED, r.status)
    }

    @Test
    fun `solicitacao aberta com prazo vencido aparece como atrasada`() {
        val r = created(receivedAt = LocalDateTime.now(clock).minusDays(20))
        every { repo.findAllByOrganizationIdOrderByReceivedAtDesc(org) } returns listOf(r)

        assertTrue(service.list(org, null).single().overdue)
    }

    @Test
    fun `executar com identificador divergente e recusado sem anonimizar`() {
        val r = created()

        assertThrows<InvalidOperationException> {
            service.execute(org, r.id, ExecuteDataSubjectRequestRequest("11911112222"), null)
        }
        verify(exactly = 0) { anonymization.anonymize(any(), any()) }
    }

    @Test
    fun `anonimizacao conclui com resumo e nao pode ser executada de novo`() {
        val r = created()
        every { anonymization.anonymize(org, any()) } returns AnonymizationReport(2, 5, 1, 1, 0, 0)

        val done = service.execute(org, r.id, ExecuteDataSubjectRequestRequest("+5511987654321"), null)

        assertEquals(DataSubjectRequestStatus.COMPLETED, done.status)
        assertTrue(done.resultSummary!!.contains("Leads anonimizados: 2"))
        assertThrows<InvalidOperationException> { service.execute(org, r.id, ExecuteDataSubjectRequestRequest("+5511987654321"), null) }
    }

    @Test
    fun `sem dados conclui com nenhum dado encontrado`() {
        val r = created()
        every { anonymization.anonymize(org, any()) } returns AnonymizationReport(0, 0, 0, 0, 0, 0)

        assertEquals(DataSubjectRequestService.NO_DATA, service.execute(org, r.id, ExecuteDataSubjectRequestRequest("11987654321"), null).resultSummary)
    }

    @Test
    fun `exclusao usa o servico de exclusao e acesso exige resumo`() {
        val del = created(DataSubjectRequestType.DELETION)
        every { deletion.deleteBySubject(org, any(), any(), any()) } returns LeadDeletionResult(1, 3)
        assertTrue(service.execute(org, del.id, ExecuteDataSubjectRequestRequest("11987654321"), null).resultSummary!!.contains("Cards excluídos: 1"))

        val access = created(DataSubjectRequestType.ACCESS)
        assertThrows<BusinessException> { service.execute(org, access.id, ExecuteDataSubjectRequestRequest("11987654321"), null) }
        assertEquals(
            "Relatório enviado ao titular",
            service.execute(org, access.id, ExecuteDataSubjectRequestRequest("11987654321", "Relatório enviado ao titular"), null).resultSummary
        )
    }

    @Test
    fun `rejeitar encerra com o motivo`() {
        val r = created()
        val rejected = service.reject(org, r.id, "Identidade não confirmada pela controladora", null)
        assertEquals(DataSubjectRequestStatus.REJECTED, rejected.status)
        assertFalse(rejected.overdue)
    }
}
