package com.astro.leadflow.shared.modules.chatbot.routing

import com.astro.leadflow.shared.modules.chatbot.domain.entity.BotSession
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.HandoffReason
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import com.astro.leadflow.shared.modules.chatbot.session.BusinessHoursService
import org.springframework.stereotype.Service

@Service
class HandoffEvaluator(
    private val businessHoursService: BusinessHoursService
) {
    fun evaluate(session: BotSession, output: BotOutput): HandoffReason? = when {
        !businessHoursService.isOpenNow() -> HandoffReason.BUSINESS_HOURS
        output.intent == BotIntent.REQUEST_HUMAN -> HandoffReason.REQUEST_HUMAN
        output.intent == BotIntent.READY_TO_BUY -> HandoffReason.READY_TO_BUY
        session.fallbackCount >= 3 -> HandoffReason.FALLBACK_LIMIT
        else -> null
    }
}
