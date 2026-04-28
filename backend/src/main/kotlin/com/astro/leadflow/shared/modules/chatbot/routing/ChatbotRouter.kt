package com.astro.leadflow.shared.modules.chatbot.routing

import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotInput
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import com.astro.leadflow.shared.modules.chatbot.engine.BedrockEngine
import com.astro.leadflow.shared.modules.chatbot.engine.LexEngine
import com.astro.leadflow.shared.modules.chatbot.engine.TemplateEngine
import com.astro.leadflow.shared.modules.chatbot.session.BotContextCache
import com.astro.leadflow.shared.modules.chatbot.session.BotSessionService
import com.astro.leadflow.shared.modules.chatbot.usage.AiQuotaGuard
import com.astro.leadflow.shared.modules.chatbot.usage.AiTokenTracker
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ChatbotRouter(
    private val leadRepository: LeadRepository,
    private val engineSelector: EngineSelector,
    private val lexEngine: LexEngine,
    private val bedrockEngine: BedrockEngine,
    private val templateEngine: TemplateEngine,
    private val botSessionService: BotSessionService,
    private val botContextCache: BotContextCache,
    private val handoffEvaluator: HandoffEvaluator,
    private val aiQuotaGuard: AiQuotaGuard,
    private val aiTokenTracker: AiTokenTracker
) {
    @Transactional
    fun handle(leadId: UUID, message: String): ChatbotResult {
        val lead = leadRepository.findById(leadId)
            .orElseThrow { ResourceNotFoundException("Lead", leadId) }
        val session = botSessionService.getOrCreateActive(lead)
        val context = botContextCache.get(session.id) + session.slotValues
        val input = BotInput(
            leadId = lead.id,
            organizationId = lead.organization.id,
            message = message,
            sessionId = session.id,
            slotValues = context
        )
        val selection = engineSelector.select(lead.organization.id)
        val output = route(selection, input)
        val updatedSession = botSessionService.applyOutput(session, output)
        aiTokenTracker.track(updatedSession, output.engineUsed, output.tokenUsage)
        botContextCache.put(updatedSession.id, updatedSession.slotValues)

        val handoffReason = handoffEvaluator.evaluate(updatedSession, output)
        val finalSession = if (handoffReason != null) {
            botSessionService.markHandoff(updatedSession, handoffReason)
        } else {
            updatedSession
        }

        return ChatbotResult(
            message = output.message,
            sessionId = finalSession.id,
            intent = output.intent,
            engineUsed = output.engineUsed,
            handoff = handoffReason != null,
            handoffReason = handoffReason
        )
    }

    private fun route(selection: EngineSelection, input: BotInput): BotOutput {
        if (selection.primary == EngineType.TEMPLATE) return templateEngine.handle(input)
        val lexOutput = lexEngine.handle(input)
        if (lexOutput.fallback && selection.allowBedrockFallback) {
            if (!aiQuotaGuard.canUseTokens(input.organizationId)) {
                return templateEngine.handle(input)
            }
            return bedrockEngine.handle(input)
        }
        return lexOutput
    }
}
