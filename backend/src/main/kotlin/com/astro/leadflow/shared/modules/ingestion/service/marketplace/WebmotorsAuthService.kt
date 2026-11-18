package com.astro.leadflow.shared.modules.ingestion.service.marketplace

import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredential
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredentialRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDateTime
import java.util.Base64

@Service
class WebmotorsAuthService(
    private val credentialRepository: MarketplaceCredentialRepository,
    webClientBuilder: WebClient.Builder,
    @Value("\${marketplace.webmotors.oauth-url:https://api-webmotors.sensedia.com/oauth/v1/access-token}")
    private val oauthUrl: String,
    @Value("\${marketplace.webmotors.client-id:}") private val appClientId: String,
    @Value("\${marketplace.webmotors.client-secret:}") private val appClientSecret: String
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val webClient = webClientBuilder.build()

    /** Retorna um access_token válido para a credencial informada.
     *  Usa o token em cache se ainda não expirou, caso contrário renova. */
    fun getValidToken(credential: MarketplaceCredential): String {
        if (isTokenValid(credential)) {
            return credential.wmAccessToken!!
        }
        return renewToken(credential)
    }

    private fun isTokenValid(credential: MarketplaceCredential): Boolean {
        return credential.wmAccessToken != null &&
            credential.wmTokenExpiresAt != null &&
            credential.wmTokenExpiresAt!!.isAfter(LocalDateTime.now().plusMinutes(1))
    }

    private fun renewToken(credential: MarketplaceCredential): String {
        log.info("Renovando token Webmotors para credencial id={}", credential.id)

        // Passo 1: token de aplicação via Basic Auth
        val appToken = fetchAppToken()

        // Passo 2: token de usuário usando o token de aplicação
        val userTokenResponse = fetchUserToken(appToken, credential)

        // Persistir token atualizado
        credential.wmAccessToken = userTokenResponse.accessToken
        credential.wmTokenExpiresAt = LocalDateTime.now().plusSeconds(userTokenResponse.expiresIn - 60)
        credentialRepository.save(credential)

        return userTokenResponse.accessToken
    }

    private fun fetchAppToken(): String {
        val basicAuth = Base64.getEncoder().encodeToString("$appClientId:$appClientSecret".toByteArray())
        val response = webClient.post()
            .uri(oauthUrl)
            .header("Authorization", "Basic $basicAuth")
            .retrieve()
            .bodyToMono<Map<String, Any>>()
            .block() ?: throw IllegalStateException("Resposta nula ao obter app token Webmotors")
        return response["access_token"] as? String
            ?: throw IllegalStateException("access_token ausente na resposta Webmotors")
    }

    private fun fetchUserToken(appToken: String, credential: MarketplaceCredential): WebmotorsTokenResponse {
        val email = credential.wmIntegratorEmail
            ?: throw IllegalStateException("wmIntegratorEmail não configurado para credencial id=${credential.id}")

        val response = webClient.post()
            .uri(oauthUrl)
            .header("Authorization", "Bearer $appToken")
            .header("client_id", appClientId)
            .bodyValue("username=$email")
            .retrieve()
            .bodyToMono<Map<String, Any>>()
            .block() ?: throw IllegalStateException("Resposta nula ao obter user token Webmotors")

        val accessToken = response["access_token"] as? String
            ?: throw IllegalStateException("access_token ausente na resposta de user token Webmotors")
        val expiresIn = (response["expires_in"] as? Number)?.toLong() ?: 3600L

        return WebmotorsTokenResponse(accessToken, expiresIn)
    }

    private data class WebmotorsTokenResponse(val accessToken: String, val expiresIn: Long)
}
