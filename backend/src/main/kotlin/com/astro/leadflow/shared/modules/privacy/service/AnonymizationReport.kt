package com.astro.leadflow.shared.modules.privacy.service

/** Resultado da anonimização, só com contagens (sem dado pessoal). */
data class AnonymizationReport(
    val leadsAnonymized: Int,
    val messagesRedacted: Int,
    val sessionsCleared: Int,
    val eventsCleared: Int,
    val inboxCleared: Int,
    val errorsRedacted: Int
) {
    fun isEmpty() = leadsAnonymized == 0 && errorsRedacted == 0
}
