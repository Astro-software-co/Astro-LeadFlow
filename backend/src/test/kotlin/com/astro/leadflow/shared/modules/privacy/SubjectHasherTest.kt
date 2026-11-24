package com.astro.leadflow.shared.modules.privacy

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.modules.ingestion.service.NormalizationService
import com.astro.leadflow.shared.modules.privacy.config.PrivacyProperties
import com.astro.leadflow.shared.modules.privacy.service.SubjectHasher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SubjectHasherTest {

    private val hasher = SubjectHasher(PrivacyProperties(hashSecret = "segredo-de-teste"), NormalizationService())

    @Test
    fun `mesmo telefone em formatos diferentes gera o mesmo hash, sem valor em claro`() {
        val a = hasher.hash(hasher.normalize("(11) 98765-4321"))
        val b = hasher.hash(hasher.normalize("+5511987654321"))

        assertEquals(a, b)
        assertEquals(64, a.length)
        assertFalse(a.contains("98765"))
        assertEquals("+55******4321", hasher.hint(hasher.normalize("11987654321")))
    }

    @Test
    fun `e-mail normalizado em minusculas e segredo diferente muda o hash`() {
        val subject = hasher.normalize(" Maria@Exemplo.com ")
        assertTrue(subject.isEmail)
        assertEquals("maria@exemplo.com", subject.value)
        val other = SubjectHasher(PrivacyProperties(hashSecret = "outro"), NormalizationService())
        assertNotEquals(hasher.hash(subject), other.hash(subject))
        assertEquals("m***@exemplo.com", hasher.hint(subject))
    }

    @Test
    fun `identificador vazio ou segredo ausente falham`() {
        assertThrows<BusinessException> { hasher.normalize("  ") }
        val noSecret = SubjectHasher(PrivacyProperties(hashSecret = ""), NormalizationService())
        assertThrows<IllegalStateException> { noSecret.hash(noSecret.normalize("11987654321")) }
    }
}
