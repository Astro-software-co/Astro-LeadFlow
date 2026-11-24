package com.astro.leadflow.shared.config

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.annotation.Configuration

/** Sobe só o sistema de logging do Spring (logback-spring.xml) e confere a saída real do console. */
@SpringBootTest(classes = [PiiLogCaptureTest.Empty::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ExtendWith(OutputCaptureExtension::class)
class PiiLogCaptureTest {

    @Configuration
    class Empty

    @Test
    fun `console nao imprime telefone, e-mail, token nem PII de excecao`(output: CapturedOutput) {
        LoggerFactory.getLogger("com.astro.leadflow.test.PiiLog").error(
            "Novo lead phone={} email={} Authorization: Bearer {}",
            "+5511987654321", "maria@dominio.com", "eyJhbGciOiJIUzI1NiJ9.corpo.assinatura",
            IllegalArgumentException("Número de telefone inválido: (11) 98765-4321")
        )

        val out = output.toString()
        assertTrue(out.contains("Novo lead phone=+55******4321"), out)
        listOf("5511987654321", "98765-4321", "maria@", "eyJhbGciOiJIUzI1NiJ9").forEach {
            assertFalse(out.contains(it), "vazou $it")
        }
    }
}
