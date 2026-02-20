package com.astro.leadflow.modules.iam.service

import com.astro.leadflow.modules.iam.domain.User
import com.astro.leadflow.modules.iam.domain.UserRepository
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
class TwoFactorService(
    private val userRepository: UserRepository,
    private val mailSender: JavaMailSender
) {
    @Transactional
    fun sendCode(user: User) {
        val code = generateCode()
        user.twoFactorCode = code
        user.twoFactorCodeExpiresAt = LocalDateTime.now().plusMinutes(10)
        userRepository.save(user)
        if (user.twoFactorEmailEnabled) sendCodeByEmail(user.email, code)
        if (user.twoFactorSmsEnabled && user.phoneNumber != null)
            sendCodeBySms(user.phoneNumber!!, code)
    }

    private fun sendCodeByEmail(email: String, code: String) {
        val message = SimpleMailMessage()
        message.to = email
        message.subject = "LeadFlow — Código de verificação"
        message.text = "Seu código de acesso é: $code\nExpira em 10 minutos."
        mailSender.send(message)
    }

    private fun sendCodeBySms(phoneNumber: String, code: String) {
        // TODO: integrar com AWS SNS
    }

    private fun generateCode(): String =
        String.format("%06d", SecureRandom().nextInt(1_000_000))
}