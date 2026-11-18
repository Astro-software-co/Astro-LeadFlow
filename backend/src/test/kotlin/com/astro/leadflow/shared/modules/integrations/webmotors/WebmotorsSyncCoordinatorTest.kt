package com.astro.leadflow.shared.modules.integrations.webmotors

import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.modules.integrations.webmotors.WebmotorsFixtures.accountId
import com.astro.leadflow.shared.modules.integrations.webmotors.config.WebmotorsProperties
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.WebmotorsAccountRepository
import com.astro.leadflow.shared.modules.integrations.webmotors.service.SyncReport
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsAccountService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsLeadSyncService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsSyncCoordinator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class WebmotorsSyncCoordinatorTest {

    private val repo = mockk<WebmotorsAccountRepository>(relaxed = true)
    private val accounts = mockk<WebmotorsAccountService>()
    private val sync = mockk<WebmotorsLeadSyncService>()
    private val coordinator = WebmotorsSyncCoordinator(repo, accounts, sync, WebmotorsProperties())

    @Test
    fun `conta com lease ativo e pulada`() {
        every { accounts.enabledAccountIds() } returns listOf(accountId)
        every { repo.claimLease(accountId, any(), any()) } returns 0

        assertEquals(0, coordinator.syncAll())

        verify(exactly = 0) { sync.syncAccount(any()) }
        verify(exactly = 0) { repo.releaseLease(any()) }
    }

    @Test
    fun `lease e liberado mesmo quando o sync estoura`() {
        every { accounts.enabledAccountIds() } returns listOf(accountId)
        every { repo.claimLease(accountId, any(), any()) } returns 1
        every { sync.syncAccount(accountId) } throws IllegalStateException("boom")

        assertThrows<IllegalStateException> { coordinator.syncAll() }

        verify { repo.releaseLease(accountId) }
    }

    @Test
    fun `sync manual com lease ocupado devolve erro de negocio`() {
        every { accounts.runtimeOfOrganization(any()) } returns WebmotorsFixtures.runtime()
        every { repo.claimLease(accountId, any(), any()) } returns 0

        assertThrows<InvalidOperationException> { coordinator.syncOrganization(WebmotorsFixtures.orgId) }
    }

    @Test
    fun `sync manual devolve o relatorio`() {
        every { accounts.runtimeOfOrganization(any()) } returns WebmotorsFixtures.runtime()
        every { repo.claimLease(accountId, any(), any()) } returns 1
        every { sync.syncAccount(accountId) } returns SyncReport(fetched = 2, newProposals = 1)

        assertEquals(1, coordinator.syncOrganization(WebmotorsFixtures.orgId).newProposals)
    }
}
