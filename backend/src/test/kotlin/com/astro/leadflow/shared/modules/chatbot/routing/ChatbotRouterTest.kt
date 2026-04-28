package com.astro.leadflow.shared.modules.chatbot.routing

import com.astro.leadflow.shared.modules.chatbot.domain.entity.BotSession
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.enums.HandoffReason
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import com.astro.leadflow.shared.modules.chatbot.engine.BedrockEngine
import com.astro.leadflow.shared.modules.chatbot.engine.LexEngine
import com.astro.leadflow.shared.modules.chatbot.engine.TemplateEngine
import com.astro.leadflow.shared.modules.chatbot.session.BotContextCache
import com.astro.leadflow.shared.modules.chatbot.session.BotSessionService
import com.astro.leadflow.shared.modules.chatbot.usage.AiQuotaGuard
import com.astro.leadflow.shared.modules.chatbot.usage.AiTokenTracker
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.iam.domain.Organization
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class ChatbotRouterTest {

    private val leadRepository = mockk<LeadRepository>()
    private val engineSelector = mockk<EngineSelector>()
    private val lexEngine = mockk<LexEngine>()
    private val bedrockEngine = mockk<BedrockEngine>()
    private val templateEngine = mockk<TemplateEngine>()
    private val botSessionService = mockk<BotSessionService>()
    private val botContextCache = mockk<BotContextCache>(relaxed = true)
    private val handoffEvaluator = mockk<HandoffEvaluator>()
    private val aiQuotaGuard = mockk<AiQuotaGuard>()
    private val aiTokenTracker = mockk<AiTokenTracker>(relaxed = true)
    private val organization = Organization(name = "Astro", slug = "astro")
    private val lead = Lead(organization = organization, phoneNumber = "+5511999999999", origin = "TEST")
    private val session = BotSession(organization = organization, lead = lead)
    private lateinit var router: ChatbotRouter

    @BeforeEach
    fun setUp() {
        router = ChatbotRouter(
            leadRepository,
            engineSelector,
            lexEngine,
            bedrockEngine,
            templateEngine,
            botSessionService,
            botContextCache,
            handoffEvaluator,
            aiQuotaGuard,
            aiTokenTracker
        )
        every { leadRepository.findById(lead.id) } returns Optional.of(lead)
        every { botSessionService.getOrCreateActive(lead) } returns session
        every { botSessionService.applyOutput(session, any()) } answers {
            val output = secondArg<BotOutput>()
            session.currentIntent = output.intent
            session.engineUsed = output.engineUsed
            session
        }
        every { botSessionService.markHandoff(session, any()) } returns session
        every { botContextCache.get(session.id) } returns emptyMap()
        every { handoffEvaluator.evaluate(session, any()) } returns null
        every { aiQuotaGuard.canUseTokens(any(), any()) } returns true
    }

    @Test
    fun `routes basic tier to template engine`() {
        val output = BotOutput("template", BotIntent.FALLBACK, EngineType.TEMPLATE)
        every { engineSelector.select(organization.id) } returns EngineSelection(EngineType.TEMPLATE, false)
        every { templateEngine.handle(any()) } returns output

        val result = router.handle(lead.id, "oi")

        assertEquals(EngineType.TEMPLATE, result.engineUsed)
        verify(exactly = 0) { lexEngine.handle(any()) }
    }

    @Test
    fun `routes professional tier to lex only`() {
        every { engineSelector.select(organization.id) } returns EngineSelection(EngineType.LEX, false)
        every { lexEngine.handle(any()) } returns BotOutput("lex", BotIntent.GREETING, EngineType.LEX)

        val result = router.handle(lead.id, "oi")

        assertEquals(EngineType.LEX, result.engineUsed)
        assertFalse(result.handoff)
        verify(exactly = 0) { bedrockEngine.handle(any()) }
    }

    @Test
    fun `routes enterprise fallback intent to bedrock`() {
        every { engineSelector.select(organization.id) } returns EngineSelection(EngineType.LEX, true)
        every { lexEngine.handle(any()) } returns BotOutput("fallback", BotIntent.FALLBACK, EngineType.LEX)
        every { bedrockEngine.handle(any()) } returns BotOutput("bedrock", BotIntent.VEHICLE_INQUIRY, EngineType.BEDROCK)

        val result = router.handle(lead.id, "texto dificil")

        assertEquals(EngineType.BEDROCK, result.engineUsed)
        verify(exactly = 1) { bedrockEngine.handle(any()) }
    }

    @Test
    fun `routes enterprise fallback to template when ai quota is blocked`() {
        every { engineSelector.select(organization.id) } returns EngineSelection(EngineType.LEX, true)
        every { lexEngine.handle(any()) } returns BotOutput("fallback", BotIntent.FALLBACK, EngineType.LEX)
        every { aiQuotaGuard.canUseTokens(organization.id, any()) } returns false
        every { templateEngine.handle(any()) } returns BotOutput("template", BotIntent.FALLBACK, EngineType.TEMPLATE)

        val result = router.handle(lead.id, "texto dificil")

        assertEquals(EngineType.TEMPLATE, result.engineUsed)
        verify(exactly = 0) { bedrockEngine.handle(any()) }
    }

    @Test
    fun `marks handoff when evaluator returns reason`() {
        every { engineSelector.select(organization.id) } returns EngineSelection(EngineType.LEX, false)
        every { lexEngine.handle(any()) } returns BotOutput("humano", BotIntent.REQUEST_HUMAN, EngineType.LEX)
        every { handoffEvaluator.evaluate(session, any()) } returns HandoffReason.REQUEST_HUMAN

        val result = router.handle(lead.id, "humano")

        assertTrue(result.handoff)
        assertEquals(HandoffReason.REQUEST_HUMAN, result.handoffReason)
        verify(exactly = 1) { botSessionService.markHandoff(session, HandoffReason.REQUEST_HUMAN) }
    }
}
