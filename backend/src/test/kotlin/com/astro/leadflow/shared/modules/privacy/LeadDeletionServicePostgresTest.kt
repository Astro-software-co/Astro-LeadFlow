package com.astro.leadflow.shared.modules.privacy

import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.crm.domain.LeadDeletionLogRepository
import com.astro.leadflow.shared.modules.crm.service.LeadDeletionService
import com.astro.leadflow.shared.modules.ingestion.service.NormalizationService
import com.astro.leadflow.shared.modules.privacy.PrivacyFixtures.Companion.EMAIL
import com.astro.leadflow.shared.modules.privacy.PrivacyFixtures.Companion.PHONE
import com.astro.leadflow.shared.modules.privacy.config.PrivacyProperties
import com.astro.leadflow.shared.modules.privacy.service.PrivacySubject
import com.astro.leadflow.shared.modules.privacy.service.SubjectHasher
import com.astro.leadflow.shared.support.PostgresContainerSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.test.context.TestPropertySource
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LeadDeletionService::class, SubjectHasher::class, NormalizationService::class, LeadDeletionServicePostgresTest.Props::class)
@TestPropertySource(properties = ["privacy.hash-secret=segredo-de-teste"])
class LeadDeletionServicePostgresTest : PostgresContainerSupport() {

    @TestConfiguration
    @EnableConfigurationProperties(PrivacyProperties::class)
    class Props

    @Autowired lateinit var service: LeadDeletionService
    @Autowired lateinit var logs: LeadDeletionLogRepository
    @Autowired lateinit var jdbc: JdbcTemplate

    private lateinit var fx: PrivacyFixtures

    @BeforeEach
    fun setup() {
        fx = PrivacyFixtures(jdbc)
    }

    private fun count(table: String, column: String, id: UUID) =
        jdbc.queryForObject("SELECT count(*) FROM $table WHERE $column = ?", Int::class.java, id)

    @Test
    fun `exclui o lead e tudo que depende dele, desvinculando o evento de marketplace`() {
        val org = fx.org()
        val lead = fx.lead(org)
        val session = fx.session(org, lead)
        fx.message(org, lead, session)
        val event = fx.event(lead)
        fx.inbox(org, lead)
        jdbc.update("INSERT INTO lead_stage_history (lead_id, to_stage) VALUES (?, 'WAITING_ATTENDANCE')", lead)

        val result = service.delete(org, lead, includeSamePhone = false, deletedBy = null, reason = "pedido do cliente")

        assertEquals(1, result.cardsDeleted)
        assertEquals(1, result.messagesDeleted)
        listOf("leads" to "id", "chat_messages" to "lead_id", "bot_session" to "lead_id", "webmotors_lead_inbox" to "lead_id", "lead_stage_history" to "lead_id")
            .forEach { (t, c) -> assertEquals(0, count(t, c, lead), "$t ainda tem dados") }
        val ev = jdbc.queryForMap("SELECT lead_id, raw_payload::text AS p FROM marketplace_webhook_events WHERE id = ?", event)
        assertNull(ev["lead_id"]); assertEquals("{}", ev["p"])
    }

    @Test
    fun `includeSamePhone exclui os outros cards do telefone so na organizacao`() {
        val org = fx.org()
        val other = fx.org()
        val l1 = fx.lead(org)
        val l2 = fx.lead(org, email = null)
        val keep = fx.lead(org, phone = "+5511911112222")
        val foreign = fx.lead(other)

        assertEquals(2, service.delete(org, l1, includeSamePhone = true, deletedBy = null, reason = null).cardsDeleted)

        assertEquals(0, count("leads", "id", l2))
        assertEquals(1, count("leads", "id", keep))
        assertEquals(1, count("leads", "id", foreign))
    }

    @Test
    fun `lead de outra organizacao responde nao encontrado`() {
        val lead = fx.lead(fx.org())
        assertThrows<ResourceNotFoundException> { service.delete(fx.org(), lead, false, null, null) }
        assertEquals(1, count("leads", "id", lead))
    }

    @Test
    fun `auditoria guarda hash e contagens, sem telefone`() {
        val org = fx.org()
        service.delete(org, fx.lead(org), includeSamePhone = false, deletedBy = null, reason = "duplicado")

        val log = logs.findAllByOrganizationId(org).single()
        assertEquals(64, log.subjectHash.length)
        assertFalse(log.subjectHash.contains("98765"))
        assertEquals(1, log.cardsDeleted)
        assertEquals("duplicado", log.reason)
    }

    @Test
    fun `solicitacao de exclusao por e-mail remove os leads do titular`() {
        val org = fx.org()
        val lead = fx.lead(org, phone = "+5511933334444")

        val result = service.deleteBySubject(org, PrivacySubject(EMAIL, isEmail = true), null, "solicitação LGPD")

        assertEquals(1, result.cardsDeleted)
        assertEquals(0, count("leads", "id", lead))
        assertEquals(0, service.deleteBySubject(org, PrivacySubject(PHONE, isEmail = false), null, null).cardsDeleted)
    }
}
