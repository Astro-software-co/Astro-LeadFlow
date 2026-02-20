// Resposta completa do board Kanban — uma lista por stage
data class KanbanBoardResponse(
    val stages: Map<PipelineStage, List<LeadResponse>>
)