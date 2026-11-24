package com.astro.leadflow.shared.config

import ch.qos.logback.classic.pattern.ThrowableProxyConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import com.astro.leadflow.shared.util.PiiMasker

/** `%maskedEx` no `logback-spring.xml`: stack trace com PII mascarado (mensagens de exceção podem carregar dado). */
class PiiMaskingThrowableConverter : ThrowableProxyConverter() {
    override fun convert(event: ILoggingEvent): String = PiiMasker.redact(super.convert(event))
}
