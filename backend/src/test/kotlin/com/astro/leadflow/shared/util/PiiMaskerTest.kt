package com.astro.leadflow.shared.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PiiMaskerTest {

    @Test
    fun `mascara telefone, e-mail e token`() {
        assertEquals("+55******4321", PiiMasker.maskPhone("+5511987654321"))
        assertEquals("******4321", PiiMasker.maskPhone("(11) 98765-4321"))
        assertEquals("***", PiiMasker.maskPhone(null))
        assertEquals("m***@dominio.com", PiiMasker.maskEmail("maria@dominio.com"))
        assertEquals("***", PiiMasker.maskEmail("sem-arroba"))
        assertEquals("***wxyz", PiiMasker.maskToken("abcdefghijklmnopqrstuvwxyz"))
        assertEquals("***", PiiMasker.maskToken("curto"))
    }

    @Test
    fun `redact remove telefone em varios formatos`() {
        listOf("+5511987654321", "5511987654321", "(11) 98765-4321", "11 98765-4321", "11987654321", "(11) 3456-7890")
            .forEach { raw ->
                val out = PiiMasker.redact("Novo lead inbound criado. phone=$raw org=x")
                assertFalse(out.contains(raw), "vazou $raw em: $out")
                assertTrue(out.contains(raw.filter { it.isDigit() }.takeLast(4)), out)
            }
    }

    @Test
    fun `redact remove e-mail e segredos`() {
        val out = PiiMasker.redact(
            "user=maria.silva@exemplo.com.br access_token=EAAGabc123secret refresh_token: \"r-123456789\" " +
                "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig client_secret=s3cr3t api-key=k-999 password=hunter2"
        )
        listOf("maria.silva@", "EAAGabc123secret", "r-123456789", "eyJhbGciOiJIUzI1NiJ9", "s3cr3t", "k-999", "hunter2")
            .forEach { assertFalse(out.contains(it), "vazou $it em: $out") }
        assertTrue(out.contains("m***@exemplo.com.br"), out)
    }

    @Test
    fun `redact nao altera uuid, datas e ids comuns`() {
        val text = "leadId=3f2b9c1e-1234-4567-8901-123456789012 em 2026-11-23 20:08:27 HTTP 500 externalId=460046 epoch=1732400000000"
        assertEquals(text, PiiMasker.redact(text))
    }
}
