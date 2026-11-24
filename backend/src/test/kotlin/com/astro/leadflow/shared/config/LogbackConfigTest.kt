package com.astro.leadflow.shared.config

import ch.qos.logback.classic.Logger
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration

/** G1-04: fora do prod, WARN/ERROR da aplicação também vão para o appender de banco (application_error). */
@SpringBootTest(classes = [LogbackConfigTest.Empty::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LogbackConfigTest {

    @Configuration
    class Empty

    @Test
    fun `logger da aplicacao escreve no appender de banco`() {
        val app = LoggerFactory.getLogger("com.astro.leadflow") as Logger
        assertNotNull(app.getAppender("ASYNC_DB_ERROR"), "com.astro.leadflow sem ASYNC_DB_ERROR")
        assertNotNull(app.getAppender("CONSOLE"))
    }
}
