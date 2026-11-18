package com.astro.leadflow.shared.modules.integrations.webmotors

import com.astro.leadflow.shared.modules.integrations.webmotors.config.WebmotorsProperties
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApiException
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsHttpClient
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WmCredentials
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.Base64
import java.util.UUID

class WebmotorsHttpClientTest {

    private val creds = WmCredentials(UUID.randomUUID(), "cid", "csecret", "user@x.com", "pw")
    private val seen = mutableListOf<ClientRequest>()

    private fun json(status: HttpStatus, body: String): ClientResponse =
        ClientResponse.create(status).header("Content-Type", "application/json").body(body).build()

    private fun client(handler: (ClientRequest) -> ClientResponse): WebmotorsHttpClient {
        val fn = ExchangeFunction { req -> seen += req; Mono.just(handler(req)) }
        return WebmotorsHttpClient(
            WebmotorsProperties(authUrl = "https://auth.test/oauth/v1/access-token", apiBaseUrl = "https://api.test"),
            WebClient.builder().exchangeFunction(fn),
            jacksonObjectMapper()
        )
    }

    private fun isAuth(r: ClientRequest) = r.url().path.endsWith("/access-token")
    private val tokenBody = """{"access_token":"tok-1","token_type":"bearer","expires_in":3600}"""

    @Test
    fun `login usa Basic com client_id e secret e grant password`() {
        val c = client { json(HttpStatus.CREATED, tokenBody) }
        c.login(creds)

        val req = seen.single()
        assertEquals("POST", req.method().name())
        val expected = "Basic " + Base64.getEncoder().encodeToString("cid:csecret".toByteArray())
        assertEquals(expected, req.headers().getFirst("Authorization"))
    }

    @Test
    fun `chamadas de dados enviam client_id e access_token e reutilizam o token`() {
        val c = client { r -> if (isAuth(r)) json(HttpStatus.CREATED, tokenBody) else json(HttpStatus.OK, WebmotorsFixtures.LIST_JSON) }

        val page = c.fetchPendingLeads(creds)
        c.fetchPendingLeads(creds, page = 2)

        val data = seen.filterNot(::isAuth)
        assertEquals(2, data.size)
        assertEquals(1, seen.count(::isAuth), "token deve ser reaproveitado")
        assertEquals("cid", data[0].headers().getFirst("client_id"))
        assertEquals("tok-1", data[0].headers().getFirst("access_token"))
        assertEquals("460046", page.mensagens.single().id)
        assertTrue(data[1].url().query.contains("pagina=2"))
        assertFalse(data[0].url().query?.contains("pagina") ?: false)
    }

    @Test
    fun `401 renova o token e repete uma vez`() {
        var dataCalls = 0
        val c = client { r ->
            when {
                isAuth(r) -> json(HttpStatus.CREATED, tokenBody)
                ++dataCalls == 1 -> json(HttpStatus.UNAUTHORIZED, "{}")
                else -> json(HttpStatus.OK, WebmotorsFixtures.LIST_JSON)
            }
        }

        c.fetchPendingLeads(creds)

        assertEquals(2, dataCalls)
        assertEquals(2, seen.count(::isAuth), "login inicial + relogin após 401")
    }

    @Test
    fun `erro HTTP vira WebmotorsApiException sem vazar o corpo`() {
        val c = client { r -> if (isAuth(r)) json(HttpStatus.CREATED, tokenBody) else json(HttpStatus.INTERNAL_SERVER_ERROR, """{"cliente":"Fulano 11999998888"}""") }

        val e = assertThrows<WebmotorsApiException> { c.fetchPendingLeads(creds) }

        assertEquals(500, e.httpStatus)
        assertFalse(e.message!!.contains("Fulano"))
        assertFalse(e.message!!.contains("11999998888"))
    }

    @Test
    fun `falha de login e propagada`() {
        val c = client { json(HttpStatus.UNAUTHORIZED, "{}") }
        val e = assertThrows<WebmotorsApiException> { c.login(creds) }
        assertEquals(401, e.httpStatus)
    }

    @Test
    fun `login sem access_token e erro`() {
        val c = client { json(HttpStatus.OK, "{}") }
        assertThrows<WebmotorsApiException> { c.login(creds) }
    }

    @Test
    fun `motivos de recusa leem o envelope Retorno`() {
        val c = client { r ->
            if (isAuth(r)) json(HttpStatus.CREATED, tokenBody)
            else json(HttpStatus.OK, """{"Sucesso":true,"Retorno":[{"Key":1,"Value":"Este lead é um SPAM"},{"Key":6,"Value":"Outros"}],"Mensagens":["Sucesso"]}""")
        }

        val reasons = c.refusalReasons(creds, leadType = 1)

        assertEquals(listOf(1, 6), reasons.map { it.key })
        assertTrue(seen.last().url().query.contains("tipolead=1"))
    }

    @Test
    fun `aceite e recusa usam PATCH em aceite e respeitam Sucesso false`() {
        val c = client { r ->
            if (isAuth(r)) json(HttpStatus.CREATED, tokenBody)
            else json(HttpStatus.OK, """{"Sucesso":false}""")
        }

        assertThrows<WebmotorsApiException> { c.accept(creds, "460046", 1) }

        val req = seen.last()
        assertEquals("PATCH", req.method().name())
        assertTrue(req.url().path.endsWith("/lead/v1/leads/460046/aceite"))
    }
}
