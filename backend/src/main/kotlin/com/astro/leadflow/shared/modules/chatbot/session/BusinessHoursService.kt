package com.astro.leadflow.shared.modules.chatbot.session

import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class BusinessHoursService(
    private val clock: Clock = Clock.system(ZoneId.of(SAO_PAULO_ZONE))
) {
    fun isOpenNow(): Boolean = isOpen()

    fun isOpen(now: ZonedDateTime = ZonedDateTime.now(clock).withZoneSameInstant(zone)): Boolean {
        val localTime = now.toLocalTime()
        return !localTime.isBefore(OPEN) && localTime.isBefore(CLOSE)
    }

    companion object {
        const val SAO_PAULO_ZONE = "America/Sao_Paulo"
        private val zone: ZoneId = ZoneId.of(SAO_PAULO_ZONE)
        private val OPEN: LocalTime = LocalTime.of(9, 0)
        private val CLOSE: LocalTime = LocalTime.of(18, 0)
    }
}
