package com.astro.leadflow.shared.modules.integrations.webmotors

import com.astro.leadflow.shared.modules.ingestion.service.NormalizationService
import com.astro.leadflow.shared.modules.integrations.webmotors.WebmotorsFixtures.entry
import com.astro.leadflow.shared.modules.integrations.webmotors.config.WebmotorsProperties
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.InboxStatus
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApi
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApiException
import com.astro.leadflow.shared.modules.integrations.webmotors.service.CrmLeadIntake
import com.astro.leadflow.shared.modules.integrations.webmotors.service.IncomingLead
import com.astro.leadflow.shared.modules.integrations.webmotors.service.IntakeResult
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsAccountService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsInboxService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsLeadImporter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class WebmotorsLeadImporterTest {

    private val api = mockk<WebmotorsApi>()
    private val inbox = mockk<WebmotorsInboxService>()
    private val accounts = mockk<WebmotorsAccountService>()
    private val intake = mockk<CrmLeadIntake>()
    private val importer = WebmotorsLeadImporter(
        api, inbox, accounts, intake, NormalizationService(), WebmotorsProperties(maxContactAttempts = 3)
    )
    private val mapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        every { accounts.runtimeOf(any()) } returns WebmotorsFixtures.runtime(welcome = true)
    }

    @Test
    fun `contato valido cria lead com origem WEBMOTORS e marca IMPORTED`() {
        val e = entry(InboxStatus.ACCEPTED)
        val leadId = UUID.randomUUID()
        every { inbox.get(e.id) } returns e
        every { api.fetchLeadDetail(any(), e.externalId, e.leadType) } returns
            mapper.readTree("""{"nome":"Maria","email":"m@x.com","telefone":"(11) 98765-4321"}""")
        val sent = slot<IncomingLead>()
        every { intake.intake(WebmotorsFixtures.orgId, capture(sent)) } returns IntakeResult(leadId, true)
        every { inbox.markImported(e.id, leadId) } returns e.copy(status = InboxStatus.IMPORTED, leadId = leadId)

        val result = importer.tryImport(e.id)

        assertEquals(InboxStatus.IMPORTED, result.status)
        assertEquals("+5511987654321", sent.captured.phone)
        assertEquals("WEBMOTORS", sent.captured.origin)
        assertEquals("Maria", sent.captured.name)
        assertEquals("Hyundai HB20 1.6 COMFORT 2018", sent.captured.vehicleInterest)
        assertTrue(sent.captured.sendWelcomeMessage)
        assertTrue(sent.captured.notes!!.contains("#460046"))
        assertTrue(sent.captured.notes!!.contains("Tenho interesse"))
    }

    @Test
    fun `telefone mascarado deixa a proposta aguardando contato e nao cria lead`() {
        val e = entry(InboxStatus.ACCEPTED)
        every { inbox.get(e.id) } returns e
        every { api.fetchLeadDetail(any(), any(), any()) } returns mapper.readTree("""{"nome":"Comprador","telefone":"(11) ..."}""")
        every { inbox.markContactIssue(e.id, any(), 3) } returns e.copy(status = InboxStatus.AWAITING_CONTACT, attempts = 1)

        val result = importer.tryImport(e.id)

        assertEquals(InboxStatus.AWAITING_CONTACT, result.status)
        verify(exactly = 0) { intake.intake(any(), any()) }
    }

    @Test
    fun `falha ao buscar detalhe conta tentativa`() {
        val e = entry(InboxStatus.AWAITING_CONTACT, attempts = 2)
        every { inbox.get(e.id) } returns e
        every { api.fetchLeadDetail(any(), any(), any()) } throws WebmotorsApiException("HTTP 503", 503)
        every { inbox.markContactIssue(e.id, "HTTP 503", 3) } returns e.copy(status = InboxStatus.FAILED, attempts = 3)

        val result = importer.tryImport(e.id)

        assertEquals(InboxStatus.FAILED, result.status)
    }

    @Test
    fun `proposta que nao esta aceita nao e importada`() {
        val e = entry(InboxStatus.PENDING)
        every { inbox.get(e.id) } returns e

        val result = importer.tryImport(e.id)

        assertEquals(InboxStatus.PENDING, result.status)
        verify(exactly = 0) { api.fetchLeadDetail(any(), any(), any()) }
    }
}
