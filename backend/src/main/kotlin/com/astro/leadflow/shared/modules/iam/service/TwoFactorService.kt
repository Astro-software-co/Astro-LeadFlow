package com.astro.leadflow.shared.modules.iam.service

import com.astro.leadflow.shared.modules.communication.infra.MetaClient
import com.astro.leadflow.shared.modules.iam.domain.User
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import com.astro.leadflow.shared.util.PiiMasker

@Service
class TwoFactorService(
    private val userRepository: UserRepository,
    private val mailSender: JavaMailSender,
    private val metaClient: MetaClient,
    @Value("\${app.meta.phone-number-id:}") private val systemPhoneNumberId: String,
    @Value("\${app.meta.access-token:}") private val systemAccessToken: String
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun sendCode(user: User) {
        val code = generateCode()
        user.twoFactorCode = code
        user.twoFactorCodeExpiresAt = LocalDateTime.now().plusMinutes(10)
        userRepository.save(user)

        if (user.twoFactorEmailEnabled) sendCodeByEmail(user.email, code)
        if (user.twoFactorSmsEnabled && user.phoneNumber != null) {
            sendCodeByWhatsApp(user.phoneNumber!!, code)
        }
    }

    private fun sendCodeByEmail(email: String, code: String) {
        val message = SimpleMailMessage()
        message.setTo(email)
        message.subject = "LeadFlow — Código de verificação"
        message.text = "Seu código de acesso é: $code\nExpira em 10 minutos."
        mailSender.send(message)
    }

    private fun sendCodeByWhatsApp(phoneNumber: String, code: String) {
        if (systemPhoneNumberId.isBlank() || systemAccessToken.isBlank()) {
            log.warn("Credenciais Meta não configuradas. 2FA via WhatsApp ignorado para {}", PiiMasker.maskPhone(phoneNumber))
            return
        }
        try {
            metaClient.sendTemplateMessage(
                phoneNumberId = systemPhoneNumberId,
                accessToken = systemAccessToken,
                toPhone = phoneNumber.trimStart('+'),
                templateName = "two_factor_code",
                components = listOf(
                    mapOf(
                        "type" to "body",
                        "parameters" to listOf(
                            mapOf("type" to "text", "text" to code)
                        )
                    )
                )
            )
            log.debug("Código 2FA enviado via WhatsApp para {}", PiiMasker.maskPhone(phoneNumber))
        } catch (e: Exception) {
            log.error("Falha ao enviar código 2FA via WhatsApp para {}. error={}", PiiMasker.maskPhone(phoneNumber), e.message)
        }
    }

    private fun generateCode(): String =
        String.format("%06d", SecureRandom().nextInt(1_000_000))
}