package com.astro.leadflow.shared.modules.chatbot.routing

import com.astro.leadflow.shared.modules.chatbot.domain.entity.BotSession
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.HandoffReason
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import org.springframework.stereotype.Service

@Service
class HandoffEvaluator {
    // O bot qualifica 24h; fora do horário o lead apenas aguarda na fila com o motivo real.
    fun evaluate(session: BotSession, output: BotOutput, qualified: Boolean = false): HandoffReason? = when {
        output.intent == BotIntent.REQUEST_HUMAN -> HandoffReason.REQUEST_HUMAN
        output.intent == BotIntent.READY_TO_BUY -> HandoffReason.READY_TO_BUY
        session.fallbackCount >= 3 -> HandoffReason.FALLBACK_LIMIT
        qualified -> HandoffReason.QUALIFIED
        else -> null
    }
}
