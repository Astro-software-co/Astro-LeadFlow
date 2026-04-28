package com.astro.leadflow.shared.modules.chatbot.usage

import com.astro.leadflow.shared.modules.chatbot.domain.entity.AiTokenUsage
import com.astro.leadflow.shared.modules.chatbot.domain.entity.BotSession
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.TokenUsage
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.saas.domain.UsageLog
import com.astro.leadflow.shared.modules.saas.domain.UsageLogRepository
import com.astro.leadflow.shared.modules.saas.domain.UsageMetricType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class AiTokenTracker(
    private val aiTokenUsageRepository: AiTokenUsageRepository,
    private val usageLogRepository: UsageLogRepository
) {
    @Transactional
    fun track(session: BotSession, engine: EngineType, tokenUsage: TokenUsage) {
        if (tokenUsage.total <= 0 || engine !in setOf(EngineType.LEX, EngineType.BEDROCK)) return

        aiTokenUsageRepository.save(
            AiTokenUsage(
                organization = session.organization,
                botSession = session,
                engine = engine,
                tokensInput = tokenUsage.inputTokens,
                tokensOutput = tokenUsage.outputTokens
            )
        )
        val period = YearMonth.now()
        incrementOrCreate(session.organization, UsageMetricType.AI_TOKENS_INPUT, tokenUsage.inputTokens, period)
        incrementOrCreate(session.organization, UsageMetricType.AI_TOKENS_OUTPUT, tokenUsage.outputTokens, period)
    }

    private fun incrementOrCreate(
        organization: Organization,
        metricType: UsageMetricType,
        amount: Int,
        period: YearMonth
    ) {
        if (amount <= 0) return
        val updated = usageLogRepository.incrementMetricCount(
            orgId = organization.id,
            marketplace = null,
            metricType = metricType,
            amount = amount,
            month = period.monthValue,
            year = period.year
        )
        if (updated == 0) {
            usageLogRepository.save(
                UsageLog(
                    organization = organization,
                    marketplaceName = null,
                    messagesSent = amount,
                    metricType = metricType,
                    periodMonth = period.monthValue,
                    periodYear = period.year
                )
            )
        }
    }
}
