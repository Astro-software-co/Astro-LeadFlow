package com.astro.leadflow.shared.modules.integrations.webmotors.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuração global da integração Webmotors (`integrations.webmotors.*`).
 *
 * Os defaults apontam para o ambiente de HOMOLOGAÇÃO, o único conhecido até agora.
 * Em produção, sobrescrever `auth-url` e `api-base-url` via variáveis de ambiente.
 */
@ConfigurationProperties(prefix = "integrations.webmotors")
data class WebmotorsProperties(
    /** Liga o polling agendado. Os endpoints REST funcionam independentemente. */
    val enabled: Boolean = false,

    val authUrl: String = "https://hlg-webmotors.sensedia.com/oauth/v1/access-token",
    val apiBaseUrl: String = "https://hlg-webmotors.sensedia.com",

    val readTimeoutMs: Long = 20_000,

    /** Intervalo entre ciclos de polling (todas as contas habilitadas). */
    val pollIntervalMs: Long = 300_000,
    val pollInitialDelayMs: Long = 60_000,

    /** Tempo que uma conta fica "reservada" por uma instância durante o sync. */
    val leaseSeconds: Long = 600,

    /** Teto de páginas lidas por conta a cada ciclo. */
    val maxPagesPerSync: Int = 20,

    /** Nome do query param de paginação. A collection não documenta; confirmar na homologação. */
    val pageParam: String = "pagina",

    /** Margem antes do vencimento para renovar o token. */
    val tokenSkewSeconds: Long = 60,

    /** Tentativas de obter o contato (telefone) de um lead aceito antes de marcá-lo como FAILED. */
    val maxContactAttempts: Int = 24,

    /** Chave AES-256 em Base64 (32 bytes) usada para cifrar credenciais por loja. */
    val secretsKey: String = ""
)
