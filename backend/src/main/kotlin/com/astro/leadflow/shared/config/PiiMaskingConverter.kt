package com.astro.leadflow.shared.config

import ch.qos.logback.classic.pattern.MessageConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import com.astro.leadflow.shared.util.PiiMasker

/** `%maskedMsg` no `logback-spring.xml`: mensagem de log com PII e segredos mascarados. */
class PiiMaskingConverter : MessageConverter() {
    override fun convert(event: ILoggingEvent): String = PiiMasker.redact(super.convert(event))
}
