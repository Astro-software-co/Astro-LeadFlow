package com.astro.leadflow.shared.modules.chatbot.engine.aws

import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotInput
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import com.astro.leadflow.shared.modules.chatbot.engine.LexEngine
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "app.chatbot.engine", name = ["mode"], havingValue = "aws")
class AwsLexEngine : LexEngine {
    // [AWS-PENDING] Wire Lex Runtime V2 once APP_AWS_ENABLED/CHATBOT_ENGINE_MODE are enabled for production.
    override fun handle(input: BotInput): BotOutput = BotOutput(
        message = "Lex AWS ainda não está ativo neste ambiente.",
        intent = BotIntent.FALLBACK,
        engineUsed = EngineType.LEX
    )
}
