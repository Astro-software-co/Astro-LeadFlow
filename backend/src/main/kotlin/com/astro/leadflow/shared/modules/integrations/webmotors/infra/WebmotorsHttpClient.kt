package com.astro.leadflow.shared.modules.integrations.webmotors.infra

import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmStatusRequest
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmDecisionRequest
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmKeyValue
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmEnvelope
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmLeadPage
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmTokenResponse
import com.astro.leadflow.shared.modules.integrations.webmotors.config.WebmotorsProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class WebmotorsHttpClient(
    private val props: WebmotorsProperties,
    webClientBuilder: WebClient.Builder,
    private val objectMapper: ObjectMapper
) : WebmotorsApi {

    private val log = LoggerFactory.getLogger(javaClass)
    private val webClient = webClientBuilder.build()
    private val tokens = ConcurrentHashMap<UUID, CachedToken>()

    private data class CachedToken(val value: String, val expiresAt: Instant)

    // ── Autenticação ───────────────────────────────────────────────

    override fun login(credentials: WmCredentials) {
        tokens.remove(credentials.accountId)
        token(credentials)
    }

    private fun token(c: WmCredentials): String {
        val cached = tokens[c.accountId]
        if (cached != null && cached.expiresAt.isAfter(Instant.now())) return cached.value

        val basic = Base64.getEncoder()
            .encodeToString("${c.clientId}:${c.clientSecret}".toByteArray(Charsets.UTF_8))
        val body = mapOf("username" to c.username, "password" to c.password, "grant_type" to "password")

        val raw = exchange(HttpMethod.POST, props.authUrl, body) { h ->
            h.set(HttpHeaders.AUTHORIZATION, "Basic $basic")
        }
        val parsed = objectMapper.readValue<WmTokenResponse>(raw)
        val access = parsed.accessToken
            ?: throw WebmotorsApiException("Resposta de login da Webmotors sem access_token.")

        val ttl = (parsed.expiresIn ?: DEFAULT_TTL_SECONDS) - props.tokenSkewSeconds
        tokens[c.accountId] = CachedToken(access, Instant.now().plusSeconds(ttl.coerceAtLeast(1)))
        return access
    }

    // ── Leads ──────────────────────────────────────────────────────

    override fun fetchPendingLeads(credentials: WmCredentials, page: Int?): WmLeadPage {
        val raw = authed(credentials, HttpMethod.GET, "/lead/v1/leads", query = pageQuery(page))
        return objectMapper.readValue(raw)
    }

    override fun fetchLeadDetail(credentials: WmCredentials, leadId: String, leadType: Int): JsonNode =
        objectMapper.readTree(
            authed(
                credentials, HttpMethod.GET, "/lead/v1/leads/$leadId",
                query = mapOf("id_tipo_lead" to leadType.toString())
            )
        )

    override fun accept(credentials: WmCredentials, leadId: String, leadType: Int) {
        // A collection envia codigo_motivo também no aceite, mas parece cópia do recusar; omitimos.
        val result = authed(
            credentials, HttpMethod.PATCH, "/lead/v1/leads/$leadId/aceite",
            body = WmDecisionRequest(aceito = true, tipoLead = leadType)
        )
        assertSuccess(result, "aceitar lead $leadId")
    }

    override fun refuse(credentials: WmCredentials, leadId: String, leadType: Int, reasonCode: Int, message: String?) {
        val result = authed(
            credentials, HttpMethod.PATCH, "/lead/v1/leads/$leadId/aceite",
            body = WmDecisionRequest(aceito = false, tipoLead = leadType, codigoMotivo = reasonCode, mensagem = message)
        )
        assertSuccess(result, "recusar lead $leadId")
    }

    override fun changeStatus(credentials: WmCredentials, leadId: String, leadType: Int, reasonCode: Int, message: String?) {
        val result = authed(
            credentials, HttpMethod.PATCH, "/lead/v1/leads/$leadId/status",
            body = WmStatusRequest(tipoLead = leadType, codigoMotivo = reasonCode, mensagem = message)
        )
        assertSuccess(result, "alterar status do lead $leadId")
    }

    override fun refusalReasons(credentials: WmCredentials, leadType: Int): List<WmKeyValue> {
        val raw = authed(
            credentials, HttpMethod.GET, "/lead/v1/motivos-recusa",
            query = mapOf("tipolead" to leadType.toString())
        )
        val env = objectMapper.readValue<WmEnvelope<List<WmKeyValue>>>(raw)
        return env.retorno.orEmpty()
    }

    override fun leadTypes(credentials: WmCredentials): JsonNode =
        objectMapper.readTree(authed(credentials, HttpMethod.GET, "/lead/v1/tipos"))

    override fun channels(credentials: WmCredentials): JsonNode =
        objectMapper.readTree(authed(credentials, HttpMethod.GET, "/lead/v1/canais"))

    // ── Infra HTTP ─────────────────────────────────────────────────

    private fun pageQuery(page: Int?): Map<String, String> =
        if (page == null) emptyMap() else mapOf(props.pageParam to page.toString())

    /** Chamada autenticada com um relogin automático se o token for recusado (401). */
    private fun authed(
        c: WmCredentials,
        method: HttpMethod,
        path: String,
        query: Map<String, String> = emptyMap(),
        body: Any? = null
    ): String {
        val url = UriComponentsBuilder.fromHttpUrl(props.apiBaseUrl.trimEnd('/') + path)
            .apply { query.forEach { (k, v) -> queryParam(k, v) } }
            .build().toUriString()

        fun call(): String {
            val t = token(c)
            return exchange(method, url, body) { h ->
                h.set("client_id", c.clientId)
                h.set("access_token", t)
            }
        }

        return try {
            call()
        } catch (e: WebmotorsApiException) {
            if (e.httpStatus != 401) throw e
            log.info("Token Webmotors recusado (401); renovando e repetindo uma vez.")
            tokens.remove(c.accountId)
            call()
        }
    }

    private fun assertSuccess(raw: String, action: String) {
        val ok = runCatching { objectMapper.readTree(raw).path("Sucesso").asBoolean(true) }.getOrDefault(true)
        if (!ok) throw WebmotorsApiException("Webmotors recusou a operação: $action.")
    }

    private fun exchange(
        method: HttpMethod,
        url: String,
        body: Any?,
        headers: (HttpHeaders) -> Unit
    ): String {
        // Nunca logar URL com query, headers nem corpo: há credenciais e dados pessoais.
        return try {
            val spec = webClient.method(method).uri(url)
                .headers { h ->
                    h.contentType = MediaType.APPLICATION_JSON
                    h.accept = listOf(MediaType.APPLICATION_JSON)
                    headers(h)
                }
            val req = if (body != null) spec.bodyValue(body) else spec
            req.retrieve()
                .onStatus({ it.isError }) { resp ->
                    resp.releaseBody().then(
                        reactor.core.publisher.Mono.error(
                            WebmotorsApiException(
                                "Webmotors respondeu HTTP ${resp.statusCode().value()} em ${method.name()} ${shortPath(url)}.",
                                httpStatus = resp.statusCode().value()
                            )
                        )
                    )
                }
                .bodyToMono(String::class.java)
                .defaultIfEmpty("{}")
                .block(Duration.ofMillis(props.readTimeoutMs))
                ?: "{}"
        } catch (e: WebmotorsApiException) {
            throw e
        } catch (e: Exception) {
            throw WebmotorsApiException("Falha ao chamar a Webmotors (${shortPath(url)}): ${e.javaClass.simpleName}", cause = e)
        }
    }

    private fun shortPath(url: String): String = url.substringAfter("://").substringAfter('/', "").substringBefore('?')
        .let { "/$it" }

    companion object {
        private const val DEFAULT_TTL_SECONDS = 3600L
    }
}
