package com.astro.leadflow.shared.modules.privacy

import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

/** Inserções mínimas por SQL para os testes de privacidade em Postgres real. Dados fictícios. */
class PrivacyFixtures(private val jdbc: JdbcTemplate) {

    fun org(): UUID = UUID.randomUUID().also {
        jdbc.update("INSERT INTO organizations (id, name, slug) VALUES (?, ?, ?)", it, "Loja $it", "loja-$it")
    }

    fun lead(org: UUID, phone: String = PHONE, email: String? = EMAIL): UUID = UUID.randomUUID().also {
        jdbc.update(
            "INSERT INTO leads (id, organization_id, name, phone_number, email, origin, notes, budget) VALUES (?, ?, 'Maria Teste', ?, ?, 'MERCADO_LIVRE', 'nota pessoal', 50000)",
            it, org, phone, email
        )
    }

    fun message(org: UUID, lead: UUID, session: UUID? = null): UUID = UUID.randomUUID().also {
        jdbc.update(
            "INSERT INTO chat_messages (id, lead_id, organization_id, direction, content, bot_session_id) VALUES (?, ?, ?, 'INBOUND', 'meu cpf e 123', ?)",
            it, lead, org, session
        )
    }

    fun session(org: UUID, lead: UUID): UUID = UUID.randomUUID().also {
        jdbc.update(
            "INSERT INTO bot_session (id, organization_id, lead_id, slot_values, status) VALUES (?, ?, ?, '{\"nome\":\"Maria\"}'::jsonb, 'COMPLETED')",
            it, org, lead
        )
    }

    fun event(lead: UUID): UUID = UUID.randomUUID().also {
        jdbc.update(
            "INSERT INTO marketplace_webhook_events (id, marketplace_type, raw_payload, lead_id) VALUES (?, 'MERCADO_LIVRE', '{\"phone\":\"$PHONE\"}'::jsonb, ?)",
            it, lead
        )
    }

    fun inbox(org: UUID, lead: UUID): UUID {
        val account = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO webmotors_accounts (id, organization_id, client_id_enc, client_secret_enc, username_enc, password_enc) VALUES (?, ?, 'a', 'b', 'c', 'd')",
            account, org
        )
        return UUID.randomUUID().also {
            jdbc.update(
                "INSERT INTO webmotors_lead_inbox (id, organization_id, account_id, external_id, status, message, raw_payload, lead_id) VALUES (?, ?, ?, ?, 'IMPORTED', 'Tenho interesse', '{}', ?)",
                it, org, account, "ext-$it", lead
            )
        }
    }

    fun appError(org: UUID?, message: String): Long =
        jdbc.queryForObject(
            "INSERT INTO application_error (level, logger_name, organization_id, message) VALUES ('ERROR', 'test', ?, ?) RETURNING id",
            Long::class.java, org, message
        )!!

    companion object {
        const val PHONE = "+5511987654321"
        const val EMAIL = "maria.teste@exemplo.com"
    }
}
