import com.astro.leadflow.shared.modules.crm.api.responses.LeadResponse
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage

// Resposta completa do board Kanban — uma lista por stage
data class KanbanBoardResponse(
    val stages: Map<PipelineStage, List<LeadResponse>>
)