package com.astro.leadflow.shared.modules.chatbot.controller

import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.chatbot.controller.dto.BotSessionDetailResponse
import com.astro.leadflow.shared.modules.chatbot.controller.dto.BotSessionPageResponse
import com.astro.leadflow.shared.modules.chatbot.controller.dto.ChatbotMetricsResponse
import com.astro.leadflow.shared.modules.chatbot.controller.dto.TopIntentResponse
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotSessionStatus
import com.astro.leadflow.shared.modules.chatbot.domain.enums.HandoffReason
import com.astro.leadflow.shared.modules.chatbot.session.BotSessionRepository
import com.astro.leadflow.shared.modules.chatbot.session.BotSessionService
import com.astro.leadflow.shared.modules.chatbot.usage.AiTokenUsageRepository
import com.astro.leadflow.shared.modules.communication.domain.ChatMessageRepository
import com.astro.leadflow.shared.modules.communication.mapper.ChatMessageMapper
import com.astro.leadflow.shared.util.SecurityUtils
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/chatbot")
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Chatbot — Manager", description = "Sessões, métricas e handoff manual do chatbot")
class ChatbotManagerController(
    private val botSessionRepository: BotSessionRepository,
    private val botSessionService: BotSessionService,
    private val aiTokenUsageRepository: AiTokenUsageRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatMessageMapper: ChatMessageMapper,
    private val responseMapper: ChatbotResponseMapper
) {
    @GetMapping("/sessions")
    fun sessions(
        @RequestParam(required = false) status: BotSessionStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): BotSessionPageResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val pageable = PageRequest.of(page, size.coerceIn(1, 100))
        val result = if (status == null) {
            botSessionRepository.findAllByOrganizationId(orgId, pageable)
        } else {
            botSessionRepository.findAllByOrganizationIdAndStatus(orgId, status, pageable)
        }
        return BotSessionPageResponse(
            content = result.content.map(responseMapper::toSummary),
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    @GetMapping("/sessions/{id}")
    fun sessionDetail(@PathVariable id: UUID): BotSessionDetailResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val session = botSessionRepository.findByIdAndOrganizationId(id, orgId)
            .orElseThrow { ResourceNotFoundException("BotSession", id) }
        val messages = chatMessageRepository.findAllByLeadIdOrderBySentAtAsc(session.lead.id)
            .map(chatMessageMapper::toResponse)
        return BotSessionDetailResponse(
            session = responseMapper.toSummary(session),
            slotValues = session.slotValues,
            messages = messages
        )
    }

    @GetMapping("/metrics")
    fun metrics(): ChatbotMetricsResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val total = botSessionRepository.countByOrganization(orgId)
        val fallback = botSessionRepository.countWithFallbackByOrganization(orgId)
        val handoff = botSessionRepository.countHandoffByOrganization(orgId)
        val denominator = total.takeIf { it > 0 } ?: 1
        return ChatbotMetricsResponse(
            totalSessions = total,
            fallbackRatePercent = fallback * 100.0 / denominator,
            handoffRatePercent = handoff * 100.0 / denominator,
            tokensConsumed = aiTokenUsageRepository.sumTokensByOrganization(orgId),
            topIntents = botSessionRepository.topIntentsByOrganization(orgId).map {
                TopIntentResponse(intent = it[0].toString(), count = (it[1] as Number).toLong())
            }
        )
    }

    @PostMapping("/sessions/{id}/handoff")
    fun forceHandoff(@PathVariable id: UUID): BotSessionDetailResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val session = botSessionRepository.findByIdAndOrganizationId(id, orgId)
            .orElseThrow { ResourceNotFoundException("BotSession", id) }
        botSessionService.markHandoff(session, HandoffReason.MANUAL)
        return sessionDetail(id)
    }
}
