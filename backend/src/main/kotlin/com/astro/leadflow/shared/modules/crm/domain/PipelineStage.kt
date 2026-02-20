package com.astro.leadflow.shared.modules.crm.domain

enum class PipelineStage(val displayName: String, val order: Int) {
    TALKING_TO_BOT("Falando com Chatbot", 1),
    WAITING_ATTENDANCE("Aguardando atendimento", 2),
    WAITING_DOCUMENTS("Aguardando documentação", 3),
    IN_NEGOTIATION("Em negociação", 4),
    COMPLETED("Concluído", 5),
    ARCHIVED("Arquivado", 6);

    fun next(): PipelineStage? =
        entries.firstOrNull { it.order == this.order + 1 && it != ARCHIVED }
}