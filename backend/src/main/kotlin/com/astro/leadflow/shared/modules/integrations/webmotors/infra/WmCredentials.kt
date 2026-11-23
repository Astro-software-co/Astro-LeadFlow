package com.astro.leadflow.shared.modules.integrations.webmotors.infra

import java.util.UUID

/** Credenciais já decifradas de uma conta Webmotors. [accountId] só serve de chave para o cache de token. */
data class WmCredentials(
    val accountId: UUID,
    val clientId: String,
    val clientSecret: String,
    val username: String,
    val password: String
) {
    override fun toString() = "WmCredentials(accountId=$accountId, clientId=***)"
}
