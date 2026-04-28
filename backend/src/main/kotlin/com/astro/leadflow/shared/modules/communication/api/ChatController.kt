package com.astro.leadflow.shared.modules.communication.api

import com.astro.leadflow.shared.modules.communication.api.requests.SendMessageRequest
import com.astro.leadflow.shared.modules.communication.api.responses.ChatMessageResponse
import com.astro.leadflow.shared.modules.chatbot.controller.ChatbotResponseMapper
import com.astro.leadflow.shared.modules.chatbot.controller.dto.BotSessionStatusResponse
import com.astro.leadflow.shared.modules.chatbot.session.BotSessionRepository
import com.astro.leadflow.shared.modules.communication.domain.ChatMessageRepository
import com.astro.leadflow.shared.modules.communication.mapper.ChatMessageMapper
import com.astro.leadflow.shared.modules.communication.service.MessageSenderService
import com.astro.leadflow.shared.modules.crm.service.LeadService
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.util.SecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Communication — Chat", description = "Histórico e envio de mensagens WhatsApp")
class ChatController(
    private val chatMessageRepository: ChatMessageRepository,
    private val messageSenderService: MessageSenderService,
    private val leadService: LeadService,
    private val organizationRepository: OrganizationRepository,
    private val chatMessageMapper: ChatMessageMapper,
    private val botSessionRepository: BotSessionRepository,
    private val chatbotResponseMapper: ChatbotResponseMapper
) {

    @GetMapping("/lead/{leadId}")
    @Operation(
        summary = "Histórico de mensagens de um lead",
        description = "Retorna todas as mensagens trocadas com o lead em ordem cronológica"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
        ApiResponse(responseCode = "404", description = "Lead não encontrado")
    )
    fun getHistory(
        @Parameter(description = "ID do lead") @PathVariable leadId: UUID
    ): List<ChatMessageResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        leadService.findLeadInOrg(leadId, orgId)
        return chatMessageRepository
            .findAllByLeadIdOrderBySentAtAsc(leadId)
            .map { chatMessageMapper.toResponse(it) }
    }

    @GetMapping("/lead/{leadId}/bot-session")
    @Operation(
        summary = "Sessão de bot do lead",
        description = "Retorna a sessão ativa ou a última sessão de bot associada ao lead."
    )
    fun getBotSession(@PathVariable leadId: UUID): BotSessionStatusResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        leadService.findLeadInOrg(leadId, orgId)
        val session = botSessionRepository.findFirstByLeadIdOrderByUpdatedAtDesc(leadId).orElse(null)
        return BotSessionStatusResponse(session?.let(chatbotResponseMapper::toSummary))
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Envia mensagem manual para o lead",
        description = "Permite que o vendedor envie uma mensagem diretamente ao lead via WhatsApp"
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Mensagem enviada com sucesso"),
        ApiResponse(responseCode = "400", description = "leadId obrigatório ou dados inválidos"),
        ApiResponse(responseCode = "404", description = "Lead ou organização não encontrados")
    )
    fun send(@Valid @RequestBody request: SendMessageRequest): ChatMessageResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val organization = organizationRepository.findById(orgId)
            .orElseThrow { ResourceNotFoundException("Organization", orgId) }
        val leadId = request.leadId
            ?: throw ResourceNotFoundException("Lead", "leadId é obrigatório")
        val lead = leadService.findLeadInOrg(leadId, orgId)
        return chatMessageMapper.toResponse(
            messageSenderService.sendText(lead, organization, request.message)
        )
    }
}
