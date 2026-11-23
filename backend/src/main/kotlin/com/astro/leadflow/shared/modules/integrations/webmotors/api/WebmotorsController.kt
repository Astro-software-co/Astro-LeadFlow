package com.astro.leadflow.shared.modules.integrations.webmotors.api

import com.astro.leadflow.shared.modules.integrations.webmotors.api.responses.ProposalResponse
import com.astro.leadflow.shared.modules.integrations.webmotors.api.requests.RefuseProposalRequest
import com.astro.leadflow.shared.modules.integrations.webmotors.api.requests.WebmotorsAccountRequest
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.InboxStatus
import com.astro.leadflow.shared.modules.integrations.webmotors.service.AccountCommand
import com.astro.leadflow.shared.modules.integrations.webmotors.service.AccountView
import com.astro.leadflow.shared.modules.integrations.webmotors.service.SyncReport
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsAccountService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsDecisionService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsInboxService
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsSyncCoordinator
import com.astro.leadflow.shared.util.SecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/integrations/webmotors")
@Tag(name = "Integrações — Webmotors", description = "Conta, propostas e sincronização com a Webmotors")
@PreAuthorize("hasRole('MANAGER')")
class WebmotorsController(
    private val accounts: WebmotorsAccountService,
    private val inbox: WebmotorsInboxService,
    private val decisions: WebmotorsDecisionService,
    private val coordinator: WebmotorsSyncCoordinator
) {

    @GetMapping("/account")
    @Operation(summary = "Configuração atual (sem segredos)")
    fun account(): AccountView =
        accounts.view(SecurityUtils.getCurrentOrganizationId())
            ?: throw ResourceNotFoundException("Conta Webmotors da organização", SecurityUtils.getCurrentOrganizationId())

    @PutMapping("/account")
    @Operation(summary = "Cria/atualiza credenciais e preferências", description = "Credenciais em branco mantêm o valor salvo.")
    fun saveAccount(@Valid @RequestBody req: WebmotorsAccountRequest): AccountView =
        accounts.upsert(
            SecurityUtils.getCurrentOrganizationId(),
            AccountCommand(
                clientId = req.clientId, clientSecret = req.clientSecret,
                username = req.username, password = req.password, storeId = req.storeId,
                acceptMode = req.acceptMode, welcomeMessageEnabled = req.welcomeMessageEnabled,
                enabled = req.enabled
            )
        )

    @PostMapping("/account/test")
    @Operation(summary = "Testa as credenciais fazendo login na Webmotors")
    fun testConnection(): Map<String, Boolean> {
        accounts.testConnection(SecurityUtils.getCurrentOrganizationId())
        return mapOf("ok" to true)
    }

    @PostMapping("/sync")
    @Operation(summary = "Sincroniza agora as propostas da Webmotors")
    fun sync(): SyncReport = coordinator.syncOrganization(SecurityUtils.getCurrentOrganizationId())

    @GetMapping("/proposals")
    @Operation(summary = "Propostas recebidas", description = "Filtro opcional por status (ex.: PENDING).")
    fun proposals(@RequestParam(required = false) status: InboxStatus?): List<ProposalResponse> =
        inbox.list(SecurityUtils.getCurrentOrganizationId(), status).map(ProposalResponseMapper::toResponse)

    @PostMapping("/proposals/{id}/accept")
    @Operation(summary = "Aceita a proposta na Webmotors e cria o lead no CRM")
    fun accept(@PathVariable id: UUID): ProposalResponse =
        ProposalResponseMapper.toResponse(decisions.accept(SecurityUtils.getCurrentOrganizationId(), id))

    @PostMapping("/proposals/{id}/refuse")
    @Operation(summary = "Recusa a proposta na Webmotors com um motivo")
    fun refuse(@PathVariable id: UUID, @Valid @RequestBody req: RefuseProposalRequest): ProposalResponse =
        ProposalResponseMapper.toResponse(decisions.refuse(SecurityUtils.getCurrentOrganizationId(), id, req.reasonCode!!, req.message))

    @GetMapping("/refusal-reasons")
    @Operation(summary = "Motivos de recusa aceitos pela Webmotors")
    fun refusalReasons(@RequestParam(defaultValue = "1") leadType: Int) =
        decisions.refusalReasons(SecurityUtils.getCurrentOrganizationId(), leadType)
}
