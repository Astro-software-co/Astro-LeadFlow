package com.astro.leadflow.shared.modules.ingestion.service.marketplace

import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredential
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredentialRepository
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import com.astro.leadflow.shared.modules.iam.domain.Organization
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDateTime

@Service
class MercadoLivreAuthService(
    private val credentialRepository: MarketplaceCredentialRepository,
    webClientBuilder: WebClient.Builder,
    @Value("\${marketplace.mercadolivre.oauth-url:https://api.mercadolibre.com/oauth/token}")
    private val oauthUrl: String,
    @Value("\${marketplace.mercadolivre.auth-url:https://auth.mercadolivre.com.br/authorization}")
    private val authUrl: String,
    @Value("\${marketplace.mercadolivre.app-id:}") private val appId: String,
    @Value("\${marketplace.mercadolivre.client-secret:}") private val clientSecret: String,
    @Value("\${marketplace.mercadolivre.redirect-uri:http://localhost:8080/api/v1/auth/mercadolivre/callback}")
    private val redirectUri: String
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val webClient = webClientBuilder.build()

    /** Gera a URL de autorização OAuth2 para redirecionar o lojista */
    fun buildAuthorizationUrl(state: String): String {
        return "$authUrl?response_type=code&client_id=$appId&redirect_uri=$redirectUri&state=$state"
    }

    /** Troca o code OAuth2 pelo par access_token + refresh_token e salva na credencial */
    fun exchangeCodeForTokens(code: String, organization: Organization): MarketplaceCredential {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", appId)
            add("client_secret", clientSecret)
            add("code", code)
            add("redirect_uri", redirectUri)
        }

        val response = postTokenRequest(params)
        log.info("Tokens ML obtidos com sucesso para org={}", organization.id)

        val credential = credentialRepository.findByMlUserIdAndMarketplaceTypeAndIsActiveTrue(
            response.userId, MarketplaceType.MERCADO_LIVRE
        ) ?: MarketplaceCredential(
            organization = organization,
            marketplaceType = MarketplaceType.MERCADO_LIVRE,
            mlAppId = appId,
            mlClientSecret = clientSecret
        )

        applyTokens(credential, response)
        return credentialRepository.save(credential)
    }

    /** Retorna um access_token válido, renovando via refresh_token se necessário */
    fun getValidToken(credential: MarketplaceCredential): String {
        if (isTokenValid(credential)) {
            return credential.mlAccessToken!!
        }
        return refreshToken(credential)
    }

    private fun isTokenValid(credential: MarketplaceCredential): Boolean {
        return credential.mlAccessToken != null &&
            credential.mlTokenExpiresAt != null &&
            credential.mlTokenExpiresAt!!.isAfter(LocalDateTime.now().plusMinutes(5))
    }

    private fun refreshToken(credential: MarketplaceCredential): String {
        val refreshToken = credential.mlRefreshToken
            ?: throw IllegalStateException("refresh_token ausente para credencial ML id=${credential.id}")

        log.info("Renovando access_token ML para credencial id={}", credential.id)

        val params = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "refresh_token")
            add("client_id", appId)
            add("client_secret", clientSecret)
            add("refresh_token", refreshToken)
        }

        val response = postTokenRequest(params)
        applyTokens(credential, response)
        credentialRepository.save(credential)

        return credential.mlAccessToken!!
    }

    private fun postTokenRequest(params: LinkedMultiValueMap<String, String>): MlTokenResponse {
        val responseBody = webClient.post()
            .uri(oauthUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(params))
            .retrieve()
            .bodyToMono<Map<String, Any?>>()
            .block() ?: throw IllegalStateException("Resposta nula ao requisitar token ML")

        val accessToken = responseBody["access_token"] as? String
            ?: throw IllegalStateException("access_token ausente na resposta ML")
        val refreshToken = responseBody["refresh_token"] as? String
        val expiresIn = (responseBody["expires_in"] as? Number)?.toLong() ?: 21600L
        val userId = (responseBody["user_id"] as? Number)?.toLong() ?: 0L

        return MlTokenResponse(accessToken, refreshToken, expiresIn, userId)
    }

    private fun applyTokens(credential: MarketplaceCredential, response: MlTokenResponse) {
        credential.mlAccessToken = response.accessToken
        if (response.refreshToken != null) credential.mlRefreshToken = response.refreshToken
        credential.mlTokenExpiresAt = LocalDateTime.now().plusSeconds(response.expiresIn - 300)
        if (response.userId != 0L) credential.mlUserId = response.userId
    }

    private data class MlTokenResponse(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Long,
        val userId: Long
    )
}
