package com.astro.leadflow.shared.modules.crm.api

import com.astro.leadflow.shared.modules.crm.service.LeadDeletionService
import com.astro.leadflow.shared.util.SecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/leads")
@Validated
@Tag(name = "Leads", description = "Gestão de leads")
class LeadDeletionController(private val leadDeletionService: LeadDeletionService) {

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
        summary = "Exclui o lead de forma irreversível",
        description = "Remove o card, mensagens, sessões do bot e propostas vinculadas. Para manter métricas, use a anonimização (/api/v1/privacy)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Lead excluído"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "403", description = "Somente MANAGER"),
        ApiResponse(responseCode = "404", description = "Lead não encontrado na organização")
    )
    fun delete(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "false") includeSamePhone: Boolean,
        @RequestParam(required = false) @Size(max = 500) reason: String?
    ) {
        leadDeletionService.delete(
            SecurityUtils.getCurrentOrganizationId(), id, includeSamePhone, SecurityUtils.getCurrentUser().id, reason
        )
    }
}
