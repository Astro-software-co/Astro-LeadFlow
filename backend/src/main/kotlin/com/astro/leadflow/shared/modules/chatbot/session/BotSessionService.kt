package com.astro.leadflow.shared.modules.chatbot.session

import com.astro.leadflow.shared.modules.chatbot.domain.entity.BotSession
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotSessionStatus
import com.astro.leadflow.shared.modules.chatbot.domain.enums.HandoffReason
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadStageHistory
import com.astro.leadflow.shared.modules.crm.domain.LeadStageHistoryRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class BotSessionService(
    private val botSessionRepository: BotSessionRepository,
    private val leadStageHistoryRepository: LeadStageHistoryRepository
) {
    @Transactional
    fun getOrCreateActive(lead: Lead): BotSession =
        botSessionRepository.findFirstByLeadIdAndStatusOrderByUpdatedAtDesc(lead.id, BotSessionStatus.ACTIVE)
            .orElseGet {
                botSessionRepository.save(
                    BotSession(
                        organization = lead.organization,
                        lead = lead
                    )
                )
            }

    @Transactional
    fun applyOutput(session: BotSession, output: BotOutput): BotSession {
        session.engineUsed = output.engineUsed
        session.currentIntent = output.intent
        session.slotValues.putAll(output.slotValues)
        session.fallbackCount = if (output.fallback) session.fallbackCount + 1 else 0
        session.lastMessageAt = OffsetDateTime.now()
        return botSessionRepository.save(session)
    }

    @Transactional
    fun markHandoff(session: BotSession, reason: HandoffReason): BotSession {
        session.markHandoff(reason)
        val lead = session.lead
        if (lead.pipelineStage == PipelineStage.TALKING_TO_BOT) {
            leadStageHistoryRepository.save(
                LeadStageHistory(
                    lead = lead,
                    fromStage = lead.pipelineStage,
                    toStage = PipelineStage.WAITING_ATTENDANCE,
                    movedBy = null
                )
            )
            lead.pipelineStage = PipelineStage.WAITING_ATTENDANCE
        }
        return botSessionRepository.save(session)
    }
}
