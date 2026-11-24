package com.astro.leadflow.shared.modules.privacy

import com.astro.leadflow.shared.modules.monitoring.config.MonitoringConfig
import com.astro.leadflow.shared.modules.privacy.config.PrivacyProperties
import com.astro.leadflow.shared.modules.privacy.service.PrivacyRetentionService
import com.astro.leadflow.shared.support.PostgresContainerSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PrivacyRetentionServicePostgresTest : PostgresContainerSupport() {

    @Autowired lateinit var jdbc: JdbcTemplate

    private val clock = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC)
    private val now = LocalDateTime.now(clock)

    @Test
    fun `limpa payloads e apaga erros alem de 90 dias, sem tocar nos recentes`() {
        val fx = PrivacyFixtures(jdbc)
        val org = fx.org()
        val oldEvent = event(now.minusDays(91))
        val newEvent = event(now.minusDays(10))
        val oldInbox = fx.inbox(org, fx.lead(org)).also { setReceived("webmotors_lead_inbox", it, now.minusDays(91)) }
        val newInbox = fx.inbox(org, fx.lead(org, phone = "+5511922223333")).also { setReceived("webmotors_lead_inbox", it, now.minusDays(1)) }
        val oldError = error(OffsetDateTime.now(clock).minusDays(91))
        val newError = error(OffsetDateTime.now(clock).minusDays(5))
        val service = PrivacyRetentionService(jdbc, PrivacyProperties(), MonitoringConfig(), clock)

        val report = service.purge()

        assertEquals(1, report.eventPayloadsCleared)
        assertEquals(1, report.inboxPayloadsCleared)
        assertEquals(1, report.errorsDeleted)
        assertEquals("{}", payload(oldEvent))
        assertEquals("{\"x\": 1}", payload(newEvent))
        assertNull(jdbc.queryForObject("SELECT raw_payload FROM webmotors_lead_inbox WHERE id = ?", String::class.java, oldInbox))
        assertNotNull(jdbc.queryForObject("SELECT raw_payload FROM webmotors_lead_inbox WHERE id = ?", String::class.java, newInbox))
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM application_error WHERE id = ?", Int::class.java, oldError))
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM application_error WHERE id = ?", Int::class.java, newError))
        assertEquals(0, service.purge().eventPayloadsCleared, "idempotente")
    }

    private fun event(receivedAt: LocalDateTime): UUID = UUID.randomUUID().also {
        jdbc.update(
            "INSERT INTO marketplace_webhook_events (id, marketplace_type, raw_payload, received_at) VALUES (?, 'MERCADO_LIVRE', '{\"x\": 1}'::jsonb, ?)",
            it, receivedAt
        )
    }

    private fun setReceived(table: String, id: UUID, at: LocalDateTime) =
        jdbc.update("UPDATE $table SET received_at = ? WHERE id = ?", at, id)

    private fun error(at: OffsetDateTime): Long = jdbc.queryForObject(
        "INSERT INTO application_error (occurred_at, level, logger_name, message) VALUES (?, 'ERROR', 't', 'm') RETURNING id",
        Long::class.java, at
    )!!

    private fun payload(id: UUID) =
        jdbc.queryForObject("SELECT raw_payload::text FROM marketplace_webhook_events WHERE id = ?", String::class.java, id)
}
