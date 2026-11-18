package com.astro.leadflow.shared.modules.integrations.webmotors

import com.astro.leadflow.shared.exception.ExternalServiceException
import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.modules.integrations.webmotors.WebmotorsFixtures.entry
import com.astro.leadflow.shared.modules.integrations.webmotors.WebmotorsFixtures.orgId
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.InboxStatus
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApi
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApiException
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsAccountService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsDecisionService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsInboxService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsLeadImporter
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class WebmotorsDecisionServiceTest {

    private val api = mockk<WebmotorsApi>()
    private val inbox = mockk<WebmotorsInboxService>()
    private val accounts = mockk<WebmotorsAccountService>()
    private val importer = mockk<WebmotorsLeadImporter>()
    private val service = WebmotorsDecisionService(api, inbox, accounts, importer)

    @BeforeEach
    fun setup() {
        every { accounts.runtimeOf(any()) } returns WebmotorsFixtures.runtime()
    }

    @Test
    fun `aceitar chama a Webmotors, marca ACCEPTED e importa`() {
        val e = entry()
        every { inbox.getForOrganization(orgId, e.id) } returns e
        every { api.accept(any(), e.externalId, e.leadType) } just Runs
        every { inbox.markAccepted(e.id) } returns e.copy(status = InboxStatus.ACCEPTED)
        every { importer.tryImport(e.id) } returns e.copy(status = InboxStatus.IMPORTED)

        val result = service.accept(orgId, e.id)

        assertEquals(InboxStatus.IMPORTED, result.status)
        verifyOrder {
            api.accept(any(), e.externalId, e.leadType)
            inbox.markAccepted(e.id)
            importer.tryImport(e.id)
        }
    }

    @Test
    fun `falha da Webmotors ao aceitar nao marca como aceita`() {
        val e = entry()
        every { inbox.getForOrganization(orgId, e.id) } returns e
        every { api.accept(any(), any(), any()) } throws WebmotorsApiException("HTTP 500", 500)

        assertThrows<ExternalServiceException> { service.accept(orgId, e.id) }

        verify(exactly = 0) { inbox.markAccepted(any()) }
    }

    @Test
    fun `proposta ja tratada nao pode ser aceita nem recusada de novo`() {
        val e = entry(InboxStatus.REFUSED)
        every { inbox.getForOrganization(orgId, e.id) } returns e

        assertThrows<InvalidOperationException> { service.accept(orgId, e.id) }
        assertThrows<InvalidOperationException> { service.refuse(orgId, e.id, 1, null) }

        verify(exactly = 0) { api.accept(any(), any(), any()) }
        verify(exactly = 0) { api.refuse(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `recusar envia o motivo e marca REFUSED`() {
        val e = entry()
        every { inbox.getForOrganization(orgId, e.id) } returns e
        every { api.refuse(any(), e.externalId, e.leadType, 3, "abaixo do valor") } just Runs
        every { inbox.markRefused(e.id, 3, "abaixo do valor") } returns e.copy(status = InboxStatus.REFUSED, refusalCode = 3)

        val result = service.refuse(orgId, e.id, 3, "abaixo do valor")

        assertEquals(InboxStatus.REFUSED, result.status)
    }

    @Test
    fun `acceptAllPending segue adiante quando uma proposta falha`() {
        val a = entry(externalId = "1")
        val b = entry(externalId = "2")
        every { inbox.idsByStatus(WebmotorsFixtures.accountId, InboxStatus.PENDING) } returns listOf(a.id, b.id)
        every { inbox.get(a.id) } returns a
        every { inbox.get(b.id) } returns b
        every { api.accept(any(), "1", any()) } throws WebmotorsApiException("HTTP 500", 500)
        every { api.accept(any(), "2", any()) } just Runs
        every { inbox.markAccepted(b.id) } returns b.copy(status = InboxStatus.ACCEPTED)
        every { importer.tryImport(b.id) } returns b.copy(status = InboxStatus.IMPORTED)

        assertEquals(1, service.acceptAllPending(WebmotorsFixtures.accountId))
    }
}
