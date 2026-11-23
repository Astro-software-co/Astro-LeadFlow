package com.astro.leadflow.shared.modules.communication.service

import com.astro.leadflow.shared.modules.communication.api.requests.MetaMessage
import com.astro.leadflow.shared.modules.communication.api.requests.MetaTextContent
import com.astro.leadflow.shared.modules.communication.domain.*
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.routing.ChatbotResult
import com.astro.leadflow.shared.modules.chatbot.routing.ChatbotRouter
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class BotServiceTest {

    private lateinit var chatMessageRepository: ChatMessageRepository
    private lateinit var leadRepository: LeadRepository
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var messageSenderService: MessageSenderService
    private lateinit var chatbotRouter: ChatbotRouter
    private lateinit var botService: BotService

    private val orgId = UUID.randomUUID()
    private val organization = Organization(id = orgId, name = "Test Motors", slug = "test-motors")
    private val phoneNumberId = "123456789"

    @BeforeEach
    fun setup() {
        chatMessageRepository = mockk(relaxed = true)
        leadRepository = mockk(relaxed = true)
        organizationRepository = mockk()
        messageSenderService = mockk(relaxed = true)
        chatbotRouter = mockk()

        botService = BotService(
            chatMessageRepository,
            leadRepository,
            organizationRepository,
            messageSenderService,
            chatbotRouter
        )

        every { chatMessageRepository.save(any<ChatMessage>()) } answers { firstArg() }
    }

    // ---- handleIncoming ----

    @Test
    fun `should route TALKING_TO_BOT inbound message through ChatbotRouter`() {
        // Given
        val lead = buildLead(name = null, vehicleInterest = null)
        val message = buildMetaMessage(body = "João Silva")
        val result = ChatbotResult(
            message = "Resposta do bot",
            sessionId = UUID.randomUUID(),
            intent = BotIntent.GREETING,
            engineUsed = EngineType.LEX,
            handoff = false
        )

        every { organizationRepository.findByWhatsappPhoneNumberId(phoneNumberId) } returns organization
        every {
            leadRepository.findFirstByOrganizationIdAndPhoneNumberOrderByCreatedAtAsc(orgId, "+5511999999999")
        } returns Optional.of(lead)
        every { chatbotRouter.handle(lead.id, "João Silva") } returns result

        // When
        botService.handleIncoming(phoneNumberId, message, null)

        // Then
        verify { chatbotRouter.handle(lead.id, "João Silva") }
        verify {
            messageSenderService.sendText(
                lead,
                organization,
                "Resposta do bot",
                null,
                ChatMessageSenderType.BOT,
                result.sessionId,
                "GREETING",
                "LEX"
            )
        }
        verify { chatMessageRepository.save(any()) }
    }

    @Test
    fun `should create new lead when lead does not exist for organization and phone number`() {
        // Given
        val message = buildMetaMessage(body = "Olá")
        val savedLead = buildLead(name = "Contact Name")

        every { organizationRepository.findByWhatsappPhoneNumberId(phoneNumberId) } returns organization
        every {
            leadRepository.findFirstByOrganizationIdAndPhoneNumberOrderByCreatedAtAsc(orgId, "+5511999999999")
        } returns Optional.empty()
        every { leadRepository.save(any()) } returns savedLead
        every { chatbotRouter.handle(savedLead.id, "Olá") } returns ChatbotResult(
            message = "Olá! Sou o bot.",
            sessionId = UUID.randomUUID(),
            intent = BotIntent.GREETING,
            engineUsed = EngineType.LEX,
            handoff = false
        )

        // When
        botService.handleIncoming(phoneNumberId, message, "Contact Name")

        // Then
        verify {
            leadRepository.save(match { lead ->
                lead.organization == organization &&
                    lead.phoneNumber == "+5511999999999" &&
                    lead.name == "Contact Name" &&
                    lead.origin == "WHATSAPP_INBOUND" &&
                    lead.pipelineStage == PipelineStage.TALKING_TO_BOT
            })
        }
    }

    @Test
    fun `should not save message or send reply when incoming message has no text content`() {
        // Given
        val message = MetaMessage(
            from = "5511999999999",
            id = "wamid.abc123",
            timestamp = "1234567890",
            type = "image",
            text = null
        )

        every { organizationRepository.findByWhatsappPhoneNumberId(phoneNumberId) } returns organization

        // When
        botService.handleIncoming(phoneNumberId, message, null)

        // Then
        verify(exactly = 0) { chatMessageRepository.save(any()) }
        verify(exactly = 0) { leadRepository.findFirstByOrganizationIdAndPhoneNumberOrderByCreatedAtAsc(any(), any()) }
        verify(exactly = 0) { messageSenderService.sendText(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `should return without processing when organization is not found for phoneNumberId`() {
        // Given
        val message = buildMetaMessage(body = "Olá")

        every { organizationRepository.findByWhatsappPhoneNumberId(phoneNumberId) } returns null

        // When
        botService.handleIncoming(phoneNumberId, message, null)

        // Then
        verify(exactly = 0) { leadRepository.findFirstByOrganizationIdAndPhoneNumberOrderByCreatedAtAsc(any(), any()) }
        verify(exactly = 0) { chatMessageRepository.save(any()) }
        verify(exactly = 0) { messageSenderService.sendText(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `should not send reply when lead is in WAITING_ATTENDANCE stage`() {
        // Given
        val lead = buildLead(
            name = "João",
            vehicleInterest = "Civic",
            stage = PipelineStage.WAITING_ATTENDANCE
        )
        val message = buildMetaMessage(body = "Alguma novidade?")

        every { organizationRepository.findByWhatsappPhoneNumberId(phoneNumberId) } returns organization
        every {
            leadRepository.findFirstByOrganizationIdAndPhoneNumberOrderByCreatedAtAsc(orgId, "+5511999999999")
        } returns Optional.of(lead)

        // When
        botService.handleIncoming(phoneNumberId, message, null)

        // Then
        verify { chatMessageRepository.save(any()) }
        verify(exactly = 0) { messageSenderService.sendText(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `should not propagate exception when sendText throws`() {
        // Given
        val lead = buildLead(name = null, vehicleInterest = null)
        val message = buildMetaMessage(body = "João Silva")

        every { organizationRepository.findByWhatsappPhoneNumberId(phoneNumberId) } returns organization
        every {
            leadRepository.findFirstByOrganizationIdAndPhoneNumberOrderByCreatedAtAsc(orgId, "+5511999999999")
        } returns Optional.of(lead)
        every { chatbotRouter.handle(lead.id, "João Silva") } returns ChatbotResult(
            message = "Resposta do bot",
            sessionId = UUID.randomUUID(),
            intent = BotIntent.GREETING,
            engineUsed = EngineType.LEX,
            handoff = false
        )
        every {
            messageSenderService.sendText(any(), any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("Meta API unavailable")

        // When / Then - should not throw
        assertDoesNotThrow {
            botService.handleIncoming(phoneNumberId, message, null)
        }

        verify { chatMessageRepository.save(any()) }
    }

    // ---- handleStatusUpdate ----

    @Test
    fun `should update status from SENT to DELIVERED`() {
        // Given
        val waMessageId = "wamid.abc123"
        val chatMessage = buildChatMessage(waMessageId = waMessageId, status = MessageStatus.SENT)

        every { chatMessageRepository.findByWaMessageId(waMessageId) } returns chatMessage
        every { chatMessageRepository.save(chatMessage) } returns chatMessage

        // When
        botService.handleStatusUpdate(waMessageId, "delivered")

        // Then
        assertEquals(MessageStatus.DELIVERED, chatMessage.status)
        verify { chatMessageRepository.save(chatMessage) }
    }

    @Test
    fun `should not downgrade status from DELIVERED to SENT`() {
        // Given
        val waMessageId = "wamid.abc123"
        val chatMessage = buildChatMessage(waMessageId = waMessageId, status = MessageStatus.DELIVERED)

        every { chatMessageRepository.findByWaMessageId(waMessageId) } returns chatMessage

        // When
        botService.handleStatusUpdate(waMessageId, "sent")

        // Then
        assertEquals(MessageStatus.DELIVERED, chatMessage.status)
        verify(exactly = 0) { chatMessageRepository.save(any()) }
    }

    // ---- helpers ----

    private fun buildMetaMessage(body: String) = MetaMessage(
        from = "5511999999999",
        id = "wamid.abc123",
        timestamp = "1234567890",
        type = "text",
        text = MetaTextContent(body = body)
    )

    private fun buildLead(
        name: String? = null,
        vehicleInterest: String? = null,
        stage: PipelineStage = PipelineStage.TALKING_TO_BOT
    ) = Lead(
        id = UUID.randomUUID(),
        organization = organization,
        phoneNumber = "+5511999999999",
        origin = "WHATSAPP_INBOUND",
        name = name,
        vehicleInterest = vehicleInterest,
        pipelineStage = stage
    )

    private fun buildChatMessage(
        waMessageId: String,
        status: MessageStatus
    ): ChatMessage {
        val lead = buildLead(name = "João")
        return ChatMessage(
            lead = lead,
            organization = organization,
            direction = MessageDirection.OUTBOUND,
            content = "Test message",
            waMessageId = waMessageId,
            status = status
        )
    }
}
