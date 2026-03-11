package com.astro.leadflow.shared.modules.crm.domain

enum class PipelineStage(val displayName: String) {
    TALKING_TO_BOT("Conversando com Bot"),
    WAITING_ATTENDANCE("Aguardando Atendimento"),
    IN_ATTENDANCE("Em Atendimento"),
    IN_NEGOTIATION("Em Negociação"),
    COMPLETED("Concluído"),
    ARCHIVED("Arquivado");

    fun next(): PipelineStage? = when (this) {
        TALKING_TO_BOT      -> WAITING_ATTENDANCE
        WAITING_ATTENDANCE  -> IN_ATTENDANCE
        IN_ATTENDANCE       -> IN_NEGOTIATION
        IN_NEGOTIATION      -> COMPLETED
        COMPLETED           -> null
        ARCHIVED            -> null
    }
}