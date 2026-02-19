package com.astro.leadflow.modules.communication.api

import com.astro.leadflow.modules.communication.domain.ChatMessage
import com.astro.leadflow.modules.communication.domain.ChatMessageRepository
import com.astro.leadflow.modules.communication.api.requests.SendMessageRequest
import com.astro.leadflow.modules.communication.service.MessageSenderService
import com.astro.leadflow.modules.crm.service.LeadService
import com.astro.leadflow.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.util.SecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Communication — Chat", description = "Histórico e envio de mensagens")
class ChatController(
    private val chatMessageRepository: ChatMessageRepository,
    private val messageSenderService: MessageSenderService,
    private val leadService: LeadService,
    private val organizationRepository: OrganizationRepository
) {

    @GetMapping("/lead/{leadId}")
    @Operation(summary = "Histórico de mensagens de um lead")
    fun getHistory(@PathVariable leadId: UUID): List<ChatMessage> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        leadService.findLeadInOrg(leadId, orgId) // valida acesso
        return chatMessageRepository.findAllByLeadIdOrderBySentAtAsc(leadId)
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Vendedor envia mensagem manual para o lead")
    fun send(@Valid @RequestBody request: SendMessageRequest): ChatMessage {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val organization = organizationRepository.findById(orgId)
            .orElseThrow { ResourceNotFoundException("Organization", orgId) }

        val lead = request.leadId?.let {
            leadService.findLeadInOrg(it, orgId)
        } ?: throw ResourceNotFoundException("Lead", "leadId é obrigatório")

        return messageSenderService.sendText(lead, organization, request.message)
    }
}