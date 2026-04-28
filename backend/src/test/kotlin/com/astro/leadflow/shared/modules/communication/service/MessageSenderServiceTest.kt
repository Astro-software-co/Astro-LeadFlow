package com.astro.leadflow.shared.modules.communication.service

import com.astro.leadflow.shared.modules.communication.domain.ChatMessage
import com.astro.leadflow.shared.modules.communication.domain.ChatMessageRepository
import com.astro.leadflow.shared.modules.communication.domain.MessageDirection
import com.astro.leadflow.shared.modules.communication.domain.MessageStatus
import com.astro.leadflow.shared.modules.communication.domain.MessageType
import com.astro.leadflow.shared.modules.communication.meta.MetaApiClient
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.exception.ExternalServiceException
import com.astro.leadflow.shared.exception.QuotaExceededException
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.saas.service.UsageLimitService
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class MessageSenderServiceTest {

    private lateinit var metaClient: MetaApiClient
    private lateinit var chatMessageRepository: ChatMessageRepository
    private lateinit var usageLimitService: UsageLimitService
    private lateinit var failureLogger: MessageDeliveryFailureLogger
    private lateinit var messageSenderService: MessageSenderService

    private val orgId = UUID.randomUUID()
    private val organization = Organization(
        id = orgId,
        name = "Test Org",
        slug = "test-org",
        whatsappPhoneNumberId = "123456789",
        whatsappToken = "test-token"
    )
    private val lead = Lead(
        organization = organization,
        phoneNumber = "5511999999999",
        name = "John Doe",
        origin = "WEBMOTORS",
        pipelineStage = PipelineStage.TALKING_TO_BOT
    )

    @BeforeEach
    fun setup() {
        metaClient = mockk()
        chatMessageRepository = mockk()
        usageLimitService = mockk()
        failureLogger = mockk(relaxed = true)
        messageSenderService = MessageSenderService(
            metaClient,
            chatMessageRepository,
            usageLimitService,
            failureLogger,
            "real",
            "",
            ""
        )
    }

    // ─── sendText ───────────────────────────────────────────────────────────────

    @Test
    fun `sendText should call MetaClient and save ChatMessage when quota is available`() {
        // Given
        val waMessageId = "wamid.abc123"
        every { usageLimitService.canSendMessage(orgId, null) } returns true
        every {
            metaClient.sendText(
                phoneNumberId = "123456789",
                accessToken = "test-token",
                toPhone = "5511999999999",
                message = "Hello"
            )
        } returns waMessageId
        every { chatMessageRepository.save(any<ChatMessage>()) } answers { firstArg() }
        every { usageLimitService.incrementUsage(orgId, null) } just runs

        // When
        val result = messageSenderService.sendText(lead, organization, "Hello")

        // Then
        assertEquals(MessageDirection.OUTBOUND, result.direction)
        assertEquals("Hello", result.content)
        assertEquals(MessageType.TEXT, result.messageType)
        assertEquals(waMessageId, result.waMessageId)
        assertEquals(MessageStatus.SENT, result.status)
        verify(exactly = 1) { chatMessageRepository.save(any<ChatMessage>()) }
        verify(exactly = 1) {
            metaClient.sendText(
                phoneNumberId = "123456789",
                accessToken = "test-token",
                toPhone = "5511999999999",
                message = "Hello"
            )
        }
    }

    @Test
    fun `sendText should increment usage after successful send`() {
        // Given
        every { usageLimitService.canSendMessage(orgId, null) } returns true
        every {
            metaClient.sendText(any(), any(), any(), any())
        } returns "wamid.xyz"
        every { chatMessageRepository.save(any<ChatMessage>()) } answers { firstArg() }
        every { usageLimitService.incrementUsage(orgId, null) } just runs

        // When
        messageSenderService.sendText(lead, organization, "Test message")

        // Then
        verify(exactly = 1) { usageLimitService.incrementUsage(orgId, null) }
    }

    @Test
    fun `sendText should throw QuotaExceededException when quota is exceeded and not call MetaClient`() {
        // Given
        every { usageLimitService.canSendMessage(orgId, null) } returns false

        // When / Then
        assertThrows(QuotaExceededException::class.java) {
            messageSenderService.sendText(lead, organization, "Hello")
        }

        verify(exactly = 0) { metaClient.sendText(any(), any(), any(), any()) }
        verify(exactly = 0) { chatMessageRepository.save(any<ChatMessage>()) }
        verify(exactly = 0) { usageLimitService.incrementUsage(any(), any()) }
    }

    @Test
    fun `sendText should throw ExternalServiceException when Meta API fails and not increment usage`() {
        // Given
        every { usageLimitService.canSendMessage(orgId, null) } returns true
        every {
            metaClient.sendText(any(), any(), any(), any())
        } throws ExternalServiceException("Meta/WhatsApp", "Erro HTTP 500")

        // When / Then
        assertThrows(ExternalServiceException::class.java) {
            messageSenderService.sendText(lead, organization, "Hello")
        }

        verify(exactly = 1) { failureLogger.logFailure(lead, organization, null, any(), any()) }
        verify(exactly = 0) { chatMessageRepository.save(any<ChatMessage>()) }
        verify(exactly = 0) { usageLimitService.incrementUsage(any(), any()) }
    }

    @Test
    fun `sendText with marketplace should pass marketplace name to incrementUsage`() {
        // Given
        val marketplaceName = "WEBMOTORS"
        every { usageLimitService.canSendMessage(orgId, marketplaceName) } returns true
        every {
            metaClient.sendText(any(), any(), any(), any())
        } returns "wamid.mkt"
        every { chatMessageRepository.save(any<ChatMessage>()) } answers { firstArg() }
        every { usageLimitService.incrementUsage(orgId, marketplaceName) } just runs

        // When
        messageSenderService.sendText(lead, organization, "Hi", marketplaceName)

        // Then
        verify(exactly = 1) { usageLimitService.canSendMessage(orgId, marketplaceName) }
        verify(exactly = 1) { usageLimitService.incrementUsage(orgId, marketplaceName) }
    }

    // ─── sendTemplate ───────────────────────────────────────────────────────────

    @Test
    fun `sendTemplate should save ChatMessage with TEMPLATE type`() {
        // Given
        val waMessageId = "wamid.tmpl123"
        every { usageLimitService.canSendMessage(orgId, null) } returns true
        every {
            metaClient.sendTemplate(
                phoneNumberId = "123456789",
                accessToken = "test-token",
                toPhone = "5511999999999",
                templateName = "welcome_msg"
            )
        } returns waMessageId
        every { chatMessageRepository.save(any<ChatMessage>()) } answers { firstArg() }
        every { usageLimitService.incrementUsage(orgId, null) } just runs

        // When
        val result = messageSenderService.sendTemplate(lead, organization, "welcome_msg")

        // Then
        assertEquals(MessageDirection.OUTBOUND, result.direction)
        assertEquals("[Template: welcome_msg]", result.content)
        assertEquals(MessageType.TEMPLATE, result.messageType)
        assertEquals(waMessageId, result.waMessageId)
        assertEquals(MessageStatus.SENT, result.status)
        verify(exactly = 1) { chatMessageRepository.save(any<ChatMessage>()) }
    }

    @Test
    fun `sendTemplate should throw IllegalArgumentException when whatsappPhoneNumberId is null`() {
        // Given
        val orgWithoutPhone = Organization(
            name = "No Phone Org",
            slug = "no-phone",
            whatsappPhoneNumberId = null,
            whatsappToken = "some-token"
        )
        every { usageLimitService.canSendMessage(orgWithoutPhone.id, null) } returns true

        // When / Then
        assertThrows(IllegalArgumentException::class.java) {
            messageSenderService.sendTemplate(lead, orgWithoutPhone, "welcome_msg")
        }

        verify(exactly = 0) { metaClient.sendTemplate(any(), any(), any(), any()) }
        verify(exactly = 0) { chatMessageRepository.save(any<ChatMessage>()) }
    }
}
