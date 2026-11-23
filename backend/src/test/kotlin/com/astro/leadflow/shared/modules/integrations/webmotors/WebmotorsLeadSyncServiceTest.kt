package com.astro.leadflow.shared.modules.integrations.webmotors

import com.astro.leadflow.shared.modules.integrations.webmotors.WebmotorsFixtures.accountId
import com.astro.leadflow.shared.modules.integrations.webmotors.config.WebmotorsProperties
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.AcceptMode
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.InboxStatus
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApi
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApiException
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmAnuncio
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmLeadItem
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmLeadPage
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmLoja
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsAccountService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsDecisionService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsInboxService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsLeadImporter
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsLeadSyncService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WebmotorsLeadSyncServiceTest {

    private val api = mockk<WebmotorsApi>()
    private val accounts = mockk<WebmotorsAccountService>(relaxed = true)
    private val inbox = mockk<WebmotorsInboxService>()
    private val decisions = mockk<WebmotorsDecisionService>()
    private val importer = mockk<WebmotorsLeadImporter>()
    private val service = WebmotorsLeadSyncService(
        api, accounts, inbox, decisions, importer, WebmotorsProperties(), jacksonObjectMapper()
    )

    private fun item(id: String?, loja: String? = null) =
        WmLeadItem(id = id, tipoLead = 1, loja = loja?.let { WmLoja(it) }, anuncio = WmAnuncio(marca = "Hyundai"))

    @BeforeEach
    fun setup() {
        every { accounts.runtimeOf(accountId) } returns WebmotorsFixtures.runtime()
        every { inbox.idsByStatus(accountId, *anyVararg()) } returns emptyList()
        every { inbox.upsert(accountId, any(), any()) } answers { WebmotorsFixtures.entry() to true }
    }

    @Test
    fun `le todas as paginas e grava cada proposta`() {
        every { api.fetchPendingLeads(any(), null) } returns
            WmLeadPage(paginaAtual = 1, totalPaginas = 2, mensagens = listOf(item("1"), item("2")))
        every { api.fetchPendingLeads(any(), 2) } returns
            WmLeadPage(paginaAtual = 2, totalPaginas = 2, mensagens = listOf(item("3")))

        val report = service.syncAccount(accountId)

        assertEquals(3, report.fetched)
        assertEquals(3, report.newProposals)
        assertNull(report.error)
        verify(exactly = 3) { inbox.upsert(accountId, any(), any()) }
        verify { accounts.recordSync(accountId, true, null) }
    }

    @Test
    fun `propostas ja conhecidas nao contam como novas`() {
        every { api.fetchPendingLeads(any(), null) } returns WmLeadPage(mensagens = listOf(item("1")))
        every { inbox.upsert(accountId, any(), any()) } returns (WebmotorsFixtures.entry() to false)

        val report = service.syncAccount(accountId)

        assertEquals(1, report.fetched)
        assertEquals(0, report.newProposals)
    }

    @Test
    fun `modo MANUAL nao aceita automaticamente`() {
        every { api.fetchPendingLeads(any(), null) } returns WmLeadPage(mensagens = listOf(item("1")))

        service.syncAccount(accountId)

        verify(exactly = 0) { decisions.acceptAllPending(any()) }
    }

    @Test
    fun `modo AUTO aceita as pendentes`() {
        every { accounts.runtimeOf(accountId) } returns WebmotorsFixtures.runtime(mode = AcceptMode.AUTO)
        every { api.fetchPendingLeads(any(), null) } returns WmLeadPage(mensagens = listOf(item("1")))
        every { decisions.acceptAllPending(accountId) } returns 1

        val report = service.syncAccount(accountId)

        assertEquals(1, report.accepted)
        verify { decisions.acceptAllPending(accountId) }
    }

    @Test
    fun `ignora item sem id e item de outra loja`() {
        every { accounts.runtimeOf(accountId) } returns WebmotorsFixtures.runtime(storeId = "77")
        every { api.fetchPendingLeads(any(), null) } returns
            WmLeadPage(mensagens = listOf(item(null), item("2", loja = "999"), item("3", loja = "77")))

        val report = service.syncAccount(accountId)

        assertEquals(3, report.fetched)
        assertEquals(2, report.skipped)
        verify(exactly = 1) { inbox.upsert(accountId, any(), any()) }
    }

    @Test
    fun `retenta importacao de propostas aceitas ou aguardando contato`() {
        every { api.fetchPendingLeads(any(), null) } returns WmLeadPage()
        val a = WebmotorsFixtures.entry(InboxStatus.AWAITING_CONTACT)
        every { inbox.idsByStatus(accountId, InboxStatus.ACCEPTED, InboxStatus.AWAITING_CONTACT) } returns listOf(a.id)
        every { importer.tryImport(a.id) } returns a

        val report = service.syncAccount(accountId)

        assertEquals(1, report.retried)
        verify { importer.tryImport(a.id) }
    }

    @Test
    fun `falha da API e registrada na conta e nao propaga`() {
        every { api.fetchPendingLeads(any(), null) } throws WebmotorsApiException("HTTP 500", 500)

        val report = service.syncAccount(accountId)

        assertNotNull(report.error)
        verify { accounts.recordSync(accountId, false, "HTTP 500") }
    }
}
