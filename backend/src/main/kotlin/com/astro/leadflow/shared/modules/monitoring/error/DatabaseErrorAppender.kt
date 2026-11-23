package com.astro.leadflow.shared.modules.monitoring.error

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase

class DatabaseErrorAppender : AppenderBase<ILoggingEvent>() {
    override fun append(eventObject: ILoggingEvent) {
        if (!eventObject.level.isGreaterOrEqual(Level.WARN)) return
        runCatching { DatabaseErrorAppenderRegistry.writer?.write(eventObject) }
    }
}
