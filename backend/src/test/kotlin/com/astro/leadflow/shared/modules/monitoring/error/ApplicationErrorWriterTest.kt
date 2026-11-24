package com.astro.leadflow.shared.modules.monitoring.error

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import com.astro.leadflow.shared.modules.monitoring.domain.entity.ApplicationError
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApplicationErrorWriterTest {

    @Test
    fun `erro persistido nao carrega telefone, e-mail nem token`() {
        val repo = mockk<ApplicationErrorRepository>()
        val saved = slot<ApplicationError>()
        every { repo.save(capture(saved)) } answers { saved.captured }
        val logger = (org.slf4j.LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("com.astro.leadflow.test")
        val event = LoggingEvent(
            "fqcn", logger, Level.ERROR,
            "Falha ao enviar para {} ({}) access_token={}",
            IllegalStateException("Número de telefone inválido: 11987654321"),
            arrayOf("+5511987654321", "maria@dominio.com", "EAAGsegredo123456")
        )

        ApplicationErrorWriter(repo).write(event)

        val row = saved.captured.message + "\n" + saved.captured.stackTrace
        listOf("5511987654321", "11987654321", "maria@", "EAAGsegredo123456").forEach {
            assertFalse(row.contains(it), "vazou $it em: $row")
        }
        assertTrue(row.contains("+55******4321"), row)
    }
}
