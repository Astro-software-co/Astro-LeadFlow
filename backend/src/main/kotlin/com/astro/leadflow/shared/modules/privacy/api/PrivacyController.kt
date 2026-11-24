package com.astro.leadflow.shared.modules.privacy.api

import com.astro.leadflow.shared.modules.privacy.api.requests.CreateDataSubjectRequestRequest
import com.astro.leadflow.shared.modules.privacy.api.requests.ExecuteDataSubjectRequestRequest
import com.astro.leadflow.shared.modules.privacy.api.requests.RejectDataSubjectRequestRequest
import com.astro.leadflow.shared.modules.privacy.api.responses.DataSubjectRequestResponse
import com.astro.leadflow.shared.modules.privacy.domain.DataSubjectRequestStatus
import com.astro.leadflow.shared.modules.privacy.service.DataSubjectRequestService
import com.astro.leadflow.shared.util.SecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/privacy/requests")
@Tag(name = "Privacidade (LGPD)", description = "Solicitações de titulares: registro, anonimização, exclusão")
@PreAuthorize("hasRole('MANAGER')")
class PrivacyController(private val service: DataSubjectRequestService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma solicitação de titular", description = "O identificador não é gravado (só HMAC e dica). Prazo: privacy.sla-days.")
    fun create(@Valid @RequestBody req: CreateDataSubjectRequestRequest): DataSubjectRequestResponse =
        service.create(SecurityUtils.getCurrentOrganizationId(), req)

    @GetMapping
    @Operation(summary = "Lista as solicitações (campo overdue indica prazo vencido)")
    fun list(@RequestParam(required = false) status: DataSubjectRequestStatus?): List<DataSubjectRequestResponse> =
        service.list(SecurityUtils.getCurrentOrganizationId(), status)

    @PostMapping("/{id}/execute")
    @Operation(summary = "Executa a solicitação", description = "Anonimização e exclusão são irreversíveis. Exige o identificador de novo.")
    fun execute(@PathVariable id: UUID, @Valid @RequestBody req: ExecuteDataSubjectRequestRequest): DataSubjectRequestResponse =
        service.execute(SecurityUtils.getCurrentOrganizationId(), id, req, SecurityUtils.getCurrentUser().id)

    @PostMapping("/{id}/reject")
    @Operation(summary = "Rejeita a solicitação com motivo")
    fun reject(@PathVariable id: UUID, @Valid @RequestBody req: RejectDataSubjectRequestRequest): DataSubjectRequestResponse =
        service.reject(SecurityUtils.getCurrentOrganizationId(), id, req.reason!!, SecurityUtils.getCurrentUser().id)
}
