package com.astro.leadflow.shared.modules.integrations.webmotors

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.modules.integrations.webmotors.config.WebmotorsProperties
import com.astro.leadflow.shared.modules.integrations.webmotors.service.SecretCipher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64

class SecretCipherTest {

    private val key = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })

    @Test
    fun `round trip devolve o texto original`() {
        val cipher = SecretCipher(WebmotorsProperties(secretsKey = key))
        assertEquals("s3nh@-ç", cipher.decrypt(cipher.encrypt("s3nh@-ç")))
    }

    @Test
    fun `mesmo texto gera cifras diferentes (IV aleatorio)`() {
        val cipher = SecretCipher(WebmotorsProperties(secretsKey = key))
        assertNotEquals(cipher.encrypt("abc"), cipher.encrypt("abc"))
    }

    @Test
    fun `sem chave configurada falha com erro claro`() {
        val cipher = SecretCipher(WebmotorsProperties(secretsKey = ""))
        assertThrows<BusinessException> { cipher.encrypt("x") }
    }

    @Test
    fun `chave com tamanho errado e recusada`() {
        val short = Base64.getEncoder().encodeToString(ByteArray(16))
        assertThrows<BusinessException> { SecretCipher(WebmotorsProperties(secretsKey = short)).encrypt("x") }
    }

    @Test
    fun `texto adulterado nao decifra`() {
        val cipher = SecretCipher(WebmotorsProperties(secretsKey = key))
        val bytes = Base64.getDecoder().decode(cipher.encrypt("abc"))
        bytes[bytes.size - 1] = (bytes.last().toInt() xor 1).toByte()
        assertThrows<Exception> { cipher.decrypt(Base64.getEncoder().encodeToString(bytes)) }
    }
}
