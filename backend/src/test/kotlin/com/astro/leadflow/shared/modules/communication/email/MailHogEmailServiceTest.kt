package com.astro.leadflow.shared.modules.communication.email

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mail.javamail.JavaMailSender
import org.thymeleaf.TemplateEngine
import java.util.Properties

class MailHogEmailServiceTest {

    @Test
    fun `sendBotQuotaWarning renders template and sends warning email`() {
        val sender = mockk<JavaMailSender>(relaxed = true)
        val templateEngine = mockk<TemplateEngine>()
        val message = MimeMessage(Session.getInstance(Properties()))
        val htmlSlot = slot<org.thymeleaf.context.Context>()

        every { sender.createMimeMessage() } returns message
        every { templateEngine.process("email/bot_quota_warning", capture(htmlSlot)) } returns "<p>alerta</p>"

        MailHogEmailService(sender, templateEngine, "noreply@test.local")
            .sendBotQuotaWarning("admin@test.local", "Astro", 800, 1000)

        verify(exactly = 1) { sender.send(message) }
        assertEquals("Sua organização atingiu 80% do limite mensal de tokens de IA", message.subject)
        assertTrue(message.allRecipients.any { it.toString() == "admin@test.local" })
        assertEquals(80, htmlSlot.captured.getVariable("usagePercent"))
    }
}
