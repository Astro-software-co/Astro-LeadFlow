package com.astro.leadflow.shared.modules.crm.api

import KanbanBoardResponse
import io.swagger.v3.oas.annotations.Operation
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
    fun getBoard(): KanbanBoardResponse =
        pipelineService.getBoard()
}