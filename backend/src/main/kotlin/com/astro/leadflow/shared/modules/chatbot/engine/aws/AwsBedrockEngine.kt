package com.astro.leadflow.shared.modules.chatbot.engine.aws

import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotInput
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import com.astro.leadflow.shared.modules.chatbot.engine.BedrockEngine
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "app.chatbot.engine", name = ["mode"], havingValue = "aws")
class AwsBedrockEngine : BedrockEngine {
    // [AWS-PENDING] Wire Bedrock Runtime once Enterprise fallback is enabled for production.
    override fun handle(input: BotInput): BotOutput = BotOutput(
        message = "Bedrock AWS ainda não está ativo neste ambiente.",
        intent = BotIntent.FALLBACK,
        engineUsed = EngineType.BEDROCK
    )
}
