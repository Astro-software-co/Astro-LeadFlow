package com.astro.leadflow.shared.modules.integrations.webmotors.service

import java.util.UUID

/**
 * Única porta de saída do módulo Webmotors para o CRM. O módulo não conhece a entidade Lead;
 * quem implementa (ver CrmLeadIntakeAdapter) decide como criar/atualizar o lead.
 */
interface CrmLeadIntake {
    fun intake(organizationId: UUID, lead: IncomingLead): IntakeResult
}
