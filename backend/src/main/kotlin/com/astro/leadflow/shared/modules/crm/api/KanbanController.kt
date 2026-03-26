package com.astro.leadflow.shared.modules.crm.api

import com.astro.leadflow.shared.modules.crm.api.responses.KanbanBoardResponse
import com.astro.leadflow.shared.modules.crm.service.PipelineService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/kanban")
@Tag(name = "CRM — Kanban", description = "Board visual de leads")
class KanbanController(
    private val pipelineService: PipelineService
) {
    @GetMapping("/board")
    @Operation(summary = "Retorna o board Kanban completo da organização")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Board retornado com sucesso"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "403", description = "Sem permissão"),
        ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    )
    fun getBoard(): KanbanBoardResponse =
        pipelineService.getBoard()
}
