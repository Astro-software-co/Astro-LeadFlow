package com.astro.leadflow.shared.modules.integrations.webmotors

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.AcceptMode
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.InboxStatus
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.WebmotorsAccount
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.WebmotorsAccountRepository
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.WebmotorsLeadInbox
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.WebmotorsLeadInboxRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:wmtest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
    ]
)
class WebmotorsRepositoriesTest {

    @Autowired lateinit var orgs: OrganizationRepository
    @Autowired lateinit var accounts: WebmotorsAccountRepository
    @Autowired lateinit var inbox: WebmotorsLeadInboxRepository
    @Autowired lateinit var em: TestEntityManager

    private lateinit var org: Organization
    private lateinit var account: WebmotorsAccount

    @BeforeEach
    fun setup() {
        org = orgs.save(Organization(name = "Loja", slug = "loja"))
        account = accounts.save(
            WebmotorsAccount(
                organization = org, clientIdEnc = "a", clientSecretEnc = "b", usernameEnc = "c", passwordEnc = "d"
            )
        )
    }

    @Test
    fun `conta nasce em modo AUTO, com abordagem e habilitada`() {
        em.flush(); em.clear()
        val loaded = accounts.findByOrganizationId(org.id).orElseThrow()
        assertEquals(AcceptMode.AUTO, loaded.acceptMode)
        assertEquals(true, loaded.welcomeMessageEnabled)
        assertEquals(1, accounts.findAllByEnabledTrue().size)
    }

    @Test
    fun `lease so e concedido uma vez ate ser liberado`() {
        val now = LocalDateTime.now()
        assertEquals(1, accounts.claimLease(account.id, now, now.plusMinutes(10)))
        assertEquals(0, accounts.claimLease(account.id, now, now.plusMinutes(10)))

        accounts.releaseLease(account.id)

        assertEquals(1, accounts.claimLease(account.id, now, now.plusMinutes(10)))
    }

    @Test
    fun `lease expirado pode ser retomado por outra instancia`() {
        val now = LocalDateTime.now()
        accounts.claimLease(account.id, now.minusHours(2), now.minusHours(1))

        assertEquals(1, accounts.claimLease(account.id, now, now.plusMinutes(10)))
    }

    private fun row(externalId: String, type: Int = 1, status: InboxStatus = InboxStatus.PENDING) =
        WebmotorsLeadInbox(organization = org, account = account, externalId = externalId, leadType = type, status = status)

    @Test
    fun `mesma proposta (org, id, tipo) nao pode ser gravada duas vezes`() {
        inbox.saveAndFlush(row("100"))
        assertThrows<DataIntegrityViolationException> { inbox.saveAndFlush(row("100")) }
    }

    @Test
    fun `mesmo id com tipo diferente e outra proposta`() {
        inbox.saveAndFlush(row("100", type = 1))
        inbox.saveAndFlush(row("100", type = 2))
        assertEquals(2, inbox.findAllByOrganizationIdOrderByReceivedAtDesc(org.id).size)
    }

    @Test
    fun `consultas por status e por conta`() {
        inbox.save(row("1", status = InboxStatus.PENDING))
        inbox.save(row("2", status = InboxStatus.ACCEPTED))
        inbox.save(row("3", status = InboxStatus.AWAITING_CONTACT))
        em.flush(); em.clear()

        assertEquals(1, inbox.findAllByOrganizationIdAndStatusOrderByReceivedAtDesc(org.id, InboxStatus.PENDING).size)
        assertEquals(
            2,
            inbox.findAllByAccountIdAndStatusIn(account.id, listOf(InboxStatus.ACCEPTED, InboxStatus.AWAITING_CONTACT)).size
        )
        assertTrue(inbox.findByOrganizationIdAndExternalIdAndLeadType(org.id, "1", 1).isPresent)
        assertNull(inbox.findByOrganizationIdAndExternalIdAndLeadType(org.id, "zzz", 1).orElse(null))
    }

    @Test
    fun `proposta nao e encontrada por outra organizacao`() {
        val saved = inbox.saveAndFlush(row("555"))
        val other = orgs.save(Organization(name = "Outra", slug = "outra"))

        assertTrue(inbox.findByOrganizationIdAndId(org.id, saved.id).isPresent)
        assertTrue(inbox.findByOrganizationIdAndId(other.id, saved.id).isEmpty)
    }
}
