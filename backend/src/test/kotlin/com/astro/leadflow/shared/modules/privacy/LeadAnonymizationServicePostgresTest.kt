package com.astro.leadflow.shared.modules.privacy

import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.privacy.PrivacyFixtures.Companion.EMAIL
import com.astro.leadflow.shared.modules.privacy.PrivacyFixtures.Companion.PHONE
import com.astro.leadflow.shared.modules.privacy.service.LeadAnonymizationService
import com.astro.leadflow.shared.modules.privacy.service.PrivacySubject
import com.astro.leadflow.shared.support.PostgresContainerSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LeadAnonymizationService::class)
class LeadAnonymizationServicePostgresTest : PostgresContainerSupport() {

    @Autowired lateinit var service: LeadAnonymizationService
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var leads: LeadRepository

    private lateinit var fx: PrivacyFixtures
    private val byPhone = PrivacySubject(PHONE, isEmail = false)

    @BeforeEach
    fun setup() {
        fx = PrivacyFixtures(jdbc)
    }

    private fun col(sql: String, id: UUID): Any? = jdbc.queryForList(sql, id).single().values.single()

    @Test
    fun `anonimiza todos os leads do telefone e os dados ligados, mantendo metricas`() {
        val org = fx.org()
        val l1 = fx.lead(org)
        val l2 = fx.lead(org, email = null)
        val session = fx.session(org, l1)
        val msg = fx.message(org, l1, session)
        val event = fx.event(l1)
        val inbox = fx.inbox(org, l2)

        val report = service.anonymize(org, byPhone)

        assertEquals(2, report.leadsAnonymized)
        assertEquals(1, report.messagesRedacted)
        assertEquals(1, report.sessionsCleared)
        assertEquals(1, report.eventsCleared)
        assertEquals(1, report.inboxCleared)
        listOf(l1, l2).forEach { id ->
            val row = jdbc.queryForMap("SELECT name, phone_number, email, notes, anonymized_at, budget FROM leads WHERE id = ?", id)
            assertEquals("Titular anonimizado", row["name"])
            assertTrue((row["phone_number"] as String).startsWith("ANON-"))
            assertTrue((row["phone_number"] as String).length <= 20)
            assertNull(row["email"]); assertNull(row["notes"]); assertNotNull(row["anonymized_at"])
            assertEquals(0, BigDecimal("50000").compareTo(row["budget"] as BigDecimal), "métrica mantida")
        }
        assertEquals("[mensagem removida]", col("SELECT content FROM chat_messages WHERE id = ?", msg))
        assertEquals("{}", col("SELECT slot_values::text FROM bot_session WHERE id = ?", session))
        assertEquals("{}", col("SELECT raw_payload::text FROM marketplace_webhook_events WHERE id = ?", event))
        assertNull(col("SELECT message FROM webmotors_lead_inbox WHERE id = ?", inbox))
        assertNull(col("SELECT raw_payload FROM webmotors_lead_inbox WHERE id = ?", inbox))
    }

    @Test
    fun `outra organizacao com o mesmo telefone nao e alterada`() {
        val org = fx.org()
        val other = fx.org()
        fx.lead(org)
        val foreign = fx.lead(other)

        service.anonymize(org, byPhone)

        assertEquals(PHONE, col("SELECT phone_number FROM leads WHERE id = ?", foreign))
    }

    @Test
    fun `e idempotente e o inbound nao reassocia ao lead anonimizado`() {
        val org = fx.org()
        fx.lead(org)

        assertEquals(1, service.anonymize(org, byPhone).leadsAnonymized)
        assertEquals(0, service.anonymize(org, byPhone).leadsAnonymized)
        assertTrue(leads.findFirstByOrganizationIdAndPhoneNumberOrderByCreatedAtAsc(org, PHONE).isEmpty)
    }

    @Test
    fun `por e-mail e erros persistidos redigidos`() {
        val org = fx.org()
        val lead = fx.lead(org, phone = "+5511911112222")
        val err = fx.appError(org, "Falha para 11987654321 e $EMAIL")
        val platformErr = fx.appError(null, "erro com $PHONE")

        val byEmail = service.anonymize(org, PrivacySubject(EMAIL, isEmail = true))
        val byPhoneReport = service.anonymize(org, byPhone)

        assertEquals(1, byEmail.leadsAnonymized)
        assertNotNull(col("SELECT anonymized_at FROM leads WHERE id = ?", lead))
        val msg = jdbc.queryForObject("SELECT message FROM application_error WHERE id = ?", String::class.java, err)!!
        assertFalse(msg.contains(EMAIL) || msg.contains("11987654321"), msg)
        val platform = jdbc.queryForObject("SELECT message FROM application_error WHERE id = ?", String::class.java, platformErr)!!
        assertFalse(platform.contains(PHONE), platform)
        assertTrue(byPhoneReport.errorsRedacted >= 1)
    }

    @Test
    fun `identificador sem dados gera relatorio vazio e sem PII`() {
        val report = service.anonymize(fx.org(), byPhone)

        assertTrue(report.isEmpty())
        assertFalse(report.toString().contains("98765"))
    }
}
