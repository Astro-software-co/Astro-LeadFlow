package com.astro.leadflow.shared.modules.chatbot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.chatbot")
data class ChatbotConfig(
    val engine: Engine = Engine(),
    val aws: Aws = Aws(),
    val redisTtlMinutes: Long = 30
) {
    data class Engine(
        val mode: String = "mock"
    )

    data class Aws(
        val lex: Lex = Lex(),
        val bedrock: Bedrock = Bedrock()
    )

    data class Lex(
        val botId: String = "",
        val botAliasId: String = "",
        val localeId: String = "pt_BR"
    )

    data class Bedrock(
        val modelId: String = ""
    )
}
