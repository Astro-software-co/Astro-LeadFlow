package com.astro.leadflow.shared.modules.monitoring.error

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import com.astro.leadflow.shared.modules.monitoring.domain.entity.ApplicationError
import com.astro.leadflow.shared.modules.monitoring.domain.enums.LogLevel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class ApplicationErrorWriterTest {
    @Test
    fun `write saves warn and error log events`() {
        val repository = mockk<ApplicationErrorRepository>()
        every { repository.save(any<ApplicationError>()) } answers { firstArg() }
        val event = mockk<ILoggingEvent>()
        every { event.level } returns Level.ERROR
        every { event.loggerName } returns "test.logger"
        every { event.mdcPropertyMap } returns emptyMap()
        every { event.formattedMessage } returns "boom"
        every { event.throwableProxy } returns null

        ApplicationErrorWriter(repository).write(event)

        verify { repository.save(match { it.level == LogLevel.ERROR && it.loggerName == "test.logger" }) }
    }
}
