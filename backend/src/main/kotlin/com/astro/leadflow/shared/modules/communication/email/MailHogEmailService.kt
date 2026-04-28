package com.astro.leadflow.shared.modules.communication.email

import jakarta.mail.internet.MimeMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Service
class MailHogEmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
    @Value("\${app.mailhog.from:no-reply@astroleadflow.local}") private val from: String
) : EmailService {

    override fun sendBotQuotaWarning(
        to: String,
        organizationName: String,
        usedTokens: Int,
        maxTokens: Int
    ) {
        val context = Context().apply {
            setVariable("organizationName", organizationName)
            setVariable("usedTokens", usedTokens)
            setVariable("maxTokens", maxTokens)
            setVariable("usagePercent", if (maxTokens <= 0) 0 else usedTokens * 100 / maxTokens)
        }
        val html = templateEngine.process("email/bot_quota_warning", context)
        mailSender.send(buildMessage(to, html))
    }

    private fun buildMessage(to: String, html: String): MimeMessage {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, "UTF-8")
        helper.setFrom(from)
        helper.setTo(to)
        helper.setSubject("Sua organização atingiu 80% do limite mensal de tokens de IA")
        helper.setText(html, true)
        return message
    }
}
