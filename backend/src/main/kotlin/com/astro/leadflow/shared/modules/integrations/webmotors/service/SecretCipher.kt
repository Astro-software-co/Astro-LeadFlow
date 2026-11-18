package com.astro.leadflow.shared.modules.integrations.webmotors.service

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.modules.integrations.webmotors.config.WebmotorsProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** AES-256-GCM para credenciais por loja. Saída: Base64(iv[12] || ciphertext+tag). */
@Component
class SecretCipher(private val props: WebmotorsProperties) {

    private val random = SecureRandom()

    fun encrypt(plain: String): String {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        }
        return Base64.getEncoder().encodeToString(iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }

    fun decrypt(encoded: String): String {
        val bytes = Base64.getDecoder().decode(encoded)
        val cipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes.copyOfRange(0, IV_BYTES)))
        }
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    }

    private fun key(): SecretKeySpec {
        if (props.secretsKey.isBlank())
            throw BusinessException(
                "Chave de criptografia da integração Webmotors não configurada (integrations.webmotors.secrets-key).",
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        val raw = runCatching { Base64.getDecoder().decode(props.secretsKey) }.getOrNull()
        if (raw == null || raw.size != 32)
            throw BusinessException(
                "integrations.webmotors.secrets-key deve ser Base64 de exatamente 32 bytes.",
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        return SecretKeySpec(raw, "AES")
    }

    private companion object {
        const val ALGORITHM = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
