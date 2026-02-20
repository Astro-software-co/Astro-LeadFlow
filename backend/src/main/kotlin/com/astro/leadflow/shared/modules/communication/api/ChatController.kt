package com.astro.leadflow.modules.communication.api

import com.astro.leadflow.shared.modules.communication.api.requests.SendMessageRequest
import com.astro.leadflow.modules.communication.api.responses.ChatMessageResponse
import com.astro.leadflow.modules.communication.mapper.ChatMessageMapper
import com.astro.leadflow.modules.communication.domain.ChatMessageRepository
import com.astro.leadflow.modules.communication.service.MessageSenderService
import com.astro.leadflow.shared.modules.crm.service.LeadService
import com.astro.leadflow.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.util.SecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
    private val chatMessageMapper: ChatMessageMapper
) {

    @GetMapping("/lead/{leadId}")
    @Operation(
        summary = "Histórico de mensagens de um lead",
        description = "Retorna todas as mensagens trocadas com o lead em ordem cronológica"
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

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Envia mensagem manual para o lead",
        description = "Permite que o vendedor envie uma mensagem diretamente ao lead via WhatsApp"
    )
    fun send(@Valid @RequestBody request: SendMessageRequest): ChatMessageResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val organization = organizationRepository.findById(orgId)
            .orElseThrow { ResourceNotFoundException("Organization", orgId) }
        val lead = leadService.findLeadInOrg(
            request.leadId ?: throw ResourceNotFoundException("Lead", "leadId obrigatório"),
            orgId
        )
        return chatMessageMapper.toResponse(
            messageSenderService.sendText(lead, organization, request.message)
        )
    }
}