package com.astro.leadflow.shared.util

/**
 * Máscaras de dado pessoal e de segredo para log. [redact] é a rede de segurança aplicada a toda
 * linha de log (console e `application_error`); as funções `mask*` servem para logar um valor
 * conhecido de forma explícita.
 */
object PiiMasker {

    private const val HIDDEN = "***"

    private val SECRET = Regex(
        """(?i)\b(access_token|refresh_token|client_secret|authorization|api[-_]?key|x-api-key|password|senha)\b(["']?\s*[:=]\s*["']?)(bearer\s+)?([^\s"',&;}]+)"""
    )
    private val BEARER = Regex("""(?i)\bbearer\s+[A-Za-z0-9._~+/=-]{8,}""")
    private val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

    // Telefone BR com DDI 55, ou com DDD (fixo ou celular). Não casa dentro de UUID/ids (vizinhos \w ou '-').
    private val PHONE_WITH_COUNTRY = Regex("""(?<![\w+-])\+?55[\s-]?\(?[1-9]\d\)?[\s-]?9?\d{4}[\s-]?\d{4}(?![\w-])""")
    private val PHONE_LOCAL = Regex("""(?<![\w-])\(?[1-9]\d\)?[\s-]?(?:9\d{4}|[2-5]\d{3})[\s-]?\d{4}(?![\w-])""")

    /** `+5511987654321` → `+55******4321`; sem DDI, `******4321`. Largura fixa: não revela o tamanho. */
    fun maskPhone(phone: String?): String {
        val digits = phone?.filter { it.isDigit() }.orEmpty()
        if (digits.length < 8) return HIDDEN
        val withCountry = phone!!.trim().startsWith("+") || digits.length > 11
        return (if (withCountry) "+" + digits.take(2) else "") + "******" + digits.takeLast(4)
    }

    /** `maria@dominio.com` → `m***@dominio.com`. */
    fun maskEmail(email: String?): String {
        if (email.isNullOrBlank() || !email.contains('@')) return HIDDEN
        val (user, domain) = email.trim().split('@', limit = 2)
        return user.take(1) + HIDDEN + "@" + domain
    }

    /** Mantém só os 4 últimos caracteres: `***abcd`. */
    fun maskToken(token: String?): String =
        if (token == null || token.length <= 8) HIDDEN else HIDDEN + token.takeLast(4)

    /** Mascara telefones, e-mails e valores de segredos em texto livre. */
    fun redact(text: String?): String {
        if (text.isNullOrEmpty()) return text.orEmpty()
        var out = SECRET.replace(text) { m -> m.groupValues[1] + m.groupValues[2] + m.groupValues[3] + HIDDEN }
        out = BEARER.replace(out) { "Bearer $HIDDEN" }
        out = EMAIL.replace(out) { maskEmail(it.value) }
        out = PHONE_WITH_COUNTRY.replace(out) { maskPhone(it.value) }
        out = PHONE_LOCAL.replace(out) { maskPhone(it.value) }
        return out
    }
}
