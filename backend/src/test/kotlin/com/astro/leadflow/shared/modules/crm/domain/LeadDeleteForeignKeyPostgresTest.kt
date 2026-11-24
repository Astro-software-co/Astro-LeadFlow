package com.astro.leadflow.shared.modules.crm.domain

import com.astro.leadflow.shared.support.PostgresContainerSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

/** T2.0: apagar um lead com evento de marketplace e sessão de bot não pode violar FK (Postgres real). */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LeadDeleteForeignKeyPostgresTest : PostgresContainerSupport() {

    @Autowired lateinit var jdbc: JdbcTemplate

    @Test
    fun `lead com evento de marketplace e sessao de bot pode ser apagado`() {
        val org = UUID.randomUUID()
        val lead = UUID.randomUUID()
        val event = UUID.randomUUID()
        val session = UUID.randomUUID()
        jdbc.update("INSERT INTO organizations (id, name, slug) VALUES (?, 'Loja FK', ?)", org, "loja-fk-$org")
        jdbc.update(
            "INSERT INTO leads (id, organization_id, phone_number, origin) VALUES (?, ?, '+5511999990000', 'MERCADO_LIVRE')",
            lead, org
        )
        jdbc.update(
            "INSERT INTO marketplace_webhook_events (id, marketplace_type, raw_payload, lead_id) VALUES (?, 'MERCADO_LIVRE', '{}'::jsonb, ?)",
            event, lead
        )
        jdbc.update("INSERT INTO bot_session (id, organization_id, lead_id) VALUES (?, ?, ?)", session, org, lead)
        jdbc.update(
            "INSERT INTO chat_messages (id, lead_id, organization_id, direction, content, bot_session_id) VALUES (?, ?, ?, 'INBOUND', 'oi', ?)",
            UUID.randomUUID(), lead, org, session
        )

        jdbc.update("DELETE FROM leads WHERE id = ?", lead)

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM leads WHERE id = ?", Int::class.java, lead))
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM bot_session WHERE id = ?", Int::class.java, session))
        assertNull(jdbc.queryForObject("SELECT lead_id FROM marketplace_webhook_events WHERE id = ?", UUID::class.java, event))
    }
}
