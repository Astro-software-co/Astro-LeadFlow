package com.astro.leadflow.shared.modules.integrations.webmotors.service

import java.util.UUID

/** Lead pronto para entrar no CRM (contato já validado). */
data class IncomingLead(
    val name: String?,
    /** Telefone normalizado em E.164 (+55...). */
    val phone: String,
    val email: String?,
    val origin: String,
    val vehicleInterest: String?,
    val notes: String?,
    val sendWelcomeMessage: Boolean
)

data class IntakeResult(val leadId: UUID, val created: Boolean)

/**
 * Única porta de saída do módulo Webmotors para o CRM. O módulo não conhece a entidade Lead;
 * quem implementa (ver CrmLeadIntakeAdapter) decide como criar/atualizar o lead.
 */
interface CrmLeadIntake {
    fun intake(organizationId: UUID, lead: IncomingLead): IntakeResult
}
