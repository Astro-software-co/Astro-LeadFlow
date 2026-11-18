package com.astro.leadflow.shared.modules.integrations.webmotors.service

import com.fasterxml.jackson.databind.JsonNode

data class WmContact(val name: String?, val email: String?, val phone: String?)

/**
 * Extrai nome/e-mail/telefone do detalhe do lead.
 *
 * O formato exato do detalhe ainda não está confirmado (a collection sugere campos `nome`,
 * `email` e `telefone`, possivelmente aninhados). Por isso a busca é em largura e ignora as
 * subárvores de loja, anúncio e vendedor, para não confundir o nome do vendedor com o do cliente.
 */
object WebmotorsContactExtractor {

    private val IGNORED = setOf("vendedor", "loja", "anuncio")
    private val NAME_KEYS = listOf("nome", "name")
    private val EMAIL_KEYS = listOf("email", "e-mail")
    private val PHONE_KEYS = listOf("telefone", "celular", "phone", "telefoneCelular")

    fun extract(detail: JsonNode): WmContact = WmContact(
        name = find(detail, NAME_KEYS),
        email = find(detail, EMAIL_KEYS),
        phone = find(detail, PHONE_KEYS)
    )

    private fun find(root: JsonNode, keys: List<String>): String? {
        val queue = ArrayDeque<JsonNode>().apply { add(root) }
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isObject) {
                for (k in keys) {
                    val v = node.get(k)
                    if (v != null && v.isValueNode && !v.isNull && v.asText().isNotBlank()) return v.asText().trim()
                }
                node.fields().forEach { (name, child) -> if (name !in IGNORED) queue.add(child) }
            } else if (node.isArray) {
                node.forEach(queue::add)
            }
        }
        return null
    }
}
