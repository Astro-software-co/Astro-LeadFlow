package com.astro.leadflow.shared.modules.chatbot.routing

import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.enums.HandoffReason
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotInput
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import com.astro.leadflow.shared.modules.chatbot.engine.BedrockEngine
import com.astro.leadflow.shared.modules.chatbot.engine.LexEngine
import com.astro.leadflow.shared.modules.chatbot.engine.TemplateEngine
import com.astro.leadflow.shared.modules.chatbot.qualification.LeadQualificationService
import com.astro.leadflow.shared.modules.chatbot.session.BotContextCache
import com.astro.leadflow.shared.modules.chatbot.session.BotSessionService
import com.astro.leadflow.shared.modules.chatbot.usage.AiQuotaGuard
import com.astro.leadflow.shared.modules.chatbot.usage.AiTokenTracker
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import org.slf4j.LoggerFactory
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
    private val aiTokenTracker: AiTokenTracker,
    private val leadQualificationService: LeadQualificationService
) {
    private val log = LoggerFactory.getLogger(this::class.java)

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

        val qualified = leadQualificationService.apply(lead, updatedSession.slotValues)
        val handoffReason = handoffEvaluator.evaluate(updatedSession, output, qualified)
        val finalSession = if (handoffReason != null) {
            botSessionService.markHandoff(updatedSession, handoffReason)
        } else {
            updatedSession
        }

        return ChatbotResult(
            message = if (handoffReason == HandoffReason.QUALIFIED) QUALIFIED_MESSAGE else output.message,
            sessionId = finalSession.id,
            intent = output.intent,
            engineUsed = output.engineUsed,
            handoff = handoffReason != null,
            handoffReason = handoffReason
        )
    }

    // Falha de motor externo nunca pode derrubar a transação do webhook: cai para o template.
    private fun route(selection: EngineSelection, input: BotInput): BotOutput =
        try {
            routeToEngine(selection, input)
        } catch (e: Exception) {
            log.error("Falha no motor do chatbot. lead={} error={}", input.leadId, e.message)
            templateEngine.handle(input)
        }

    private fun routeToEngine(selection: EngineSelection, input: BotInput): BotOutput {
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

    private companion object {
        const val QUALIFIED_MESSAGE =
            "Anotei seu interesse. Um consultor vai continuar o atendimento com você em breve."
    }
}
