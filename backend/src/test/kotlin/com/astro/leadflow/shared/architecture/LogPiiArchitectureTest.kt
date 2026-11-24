package com.astro.leadflow.shared.architecture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * Falha se uma chamada `log.*(...)` em `src/main` usa como argumento (ou interpola na mensagem)
 * uma variável cujo nome sugere PII/segredo sem passar por `PiiMasker`.
 */
class LogPiiArchitectureTest {

    private val logCall = Regex("""\blog\.(trace|debug|info|warn|error)\(""")
    private val sensitiveName = Regex("""(?i)\b[\w.]*(phone|email|token|secret|password)\w*\b""")
    private val masked = Regex("""PiiMasker\.\w+\((?:[^()]|\([^()]*\))*\)""")
    private val stringLiteral = Regex(""""(?:\\.|[^"\\])*"""")
    private val template = Regex("""\$\{([^}]*)}|\$(\w+)""")

    /** Ids técnicos (ex.: `phoneNumberId` da Meta) e constantes (nome de header) não são dado pessoal. */
    private fun isTechnicalId(name: String) =
        name.endsWith("Id") || name.endsWith("Ids") || name.endsWith(".id") || name == name.uppercase()

    fun offenders(source: String): List<String> = logCall.findAll(source).mapNotNull { m ->
        val call = balanced(source, m.range.last) ?: return@mapNotNull null
        val withoutMasked = masked.replace(call, "")
        val interpolated = stringLiteral.findAll(withoutMasked)
            .flatMap { lit -> template.findAll(lit.value).map { it.groupValues[1] + it.groupValues[2] } }
            .joinToString(" ")
        val arguments = stringLiteral.replace(withoutMasked, "\"\"")
        val hits = sensitiveName.findAll("$arguments $interpolated").map { it.value }.filterNot(::isTechnicalId)
        hits.firstOrNull()?.let { "log.${m.groupValues[1]}(… $it …)" }
    }.toList()

    private fun balanced(text: String, openIndex: Int): String? {
        var depth = 0
        var inString = false
        var i = openIndex
        while (i < text.length) {
            val c = text[i]
            if (inString) {
                if (c == '\\') i++ else if (c == '"') inString = false
            } else when (c) {
                '"' -> inString = true
                '(' -> depth++
                ')' -> { depth--; if (depth == 0) return text.substring(openIndex, i + 1) }
            }
            i++
        }
        return null
    }

    @Test
    fun `nenhum log de producao usa PII sem PiiMasker`() {
        val found = mutableListOf<String>()
        Files.walk(Path.of("src/main/kotlin")).use { paths ->
            paths.filter { it.toString().endsWith(".kt") }.forEach { file ->
                offenders(Files.readString(file)).forEach { found += "${file.fileName}: $it" }
            }
        }
        assertTrue(found.isEmpty(), "Logs com PII sem máscara:\n" + found.joinToString("\n"))
    }

    @Test
    fun `o detector reconhece os casos que deve barrar e liberar`() {
        assertEquals(1, offenders("""log.info("Lead {}", dto.phoneNumber)""").size)
        assertEquals(1, offenders("""log.info("Processando ${'$'}{lead.phoneNumber}")""").size)
        assertEquals(1, offenders("log.error(\"falha {}\", accessToken, e)").size)
        assertEquals(0, offenders("""log.info("Lead {}", PiiMasker.maskPhone(dto.phoneNumber))""").size)
        assertEquals(0, offenders("""log.warn("Org não encontrada phoneNumberId={}", phoneNumberId)""").size)
        assertEquals(0, offenders("""log.warn("sem header {}", TOKEN_HEADER)""").size)
        assertEquals(1, offenders("""log.warn("notificados {}", adminEmails.joinToString(", "))""").size)
        assertEquals(0, offenders("""log.info("Renovando token Webmotors para credencial id={}", credential.id)""").size)
    }
}
