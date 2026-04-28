package com.astro.leadflow.shared.modules.monitoring.service

import com.astro.leadflow.shared.modules.monitoring.controller.dto.SystemHealthResponse
import org.springframework.core.env.Environment
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class SystemHealthService(
    private val dataSource: DataSource,
    private val redisTemplate: StringRedisTemplate,
    private val environment: Environment
) {

    fun current(): SystemHealthResponse {
        val chatbotMode = property("app.chatbot.engine.mode", "mock")
        return SystemHealthResponse(
            abacatePayConfigured = property("app.abacatepay.api-key").isNotBlank() &&
                property("app.abacatepay.webhook-secret").isNotBlank(),
            metaWebhookSecretConfigured = property("app.meta.webhook.app-secret").isNotBlank() ||
                property("app.meta.webhook-secret").isNotBlank(),
            metaApiTokenConfigured = property("app.meta.api.access-token").isNotBlank() ||
                property("app.meta.access-token").isNotBlank(),
            redisUp = redisUp(),
            dbUp = dbUp(),
            chatbotEngineMode = chatbotMode,
            awsLexConfigured = property("app.chatbot.aws.lex.bot-id").isNotBlank() &&
                property("app.chatbot.aws.lex.bot-alias-id").isNotBlank(),
            awsBedrockConfigured = property("app.chatbot.aws.bedrock.model-id").isNotBlank(),
            publicApiUrlConfigured = property("app.public-api-url").isNotBlank()
        )
    }

    private fun property(name: String, default: String = ""): String =
        environment.getProperty(name, default).orEmpty()

    private fun dbUp(): Boolean = runCatching {
        dataSource.connection.use { connection -> connection.isValid(1) }
    }.getOrDefault(false)

    private fun redisUp(): Boolean = runCatching {
        redisTemplate.connectionFactory?.connection?.use { connection ->
            connection.ping()
        } != null
    }.getOrDefault(false)
}
