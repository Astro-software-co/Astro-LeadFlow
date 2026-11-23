package com.astro.leadflow.shared.modules.integrations.webmotors.service

import com.astro.leadflow.shared.modules.crm.intake.LeadIntakeCommand

/**
 * Única porta de saída do módulo Webmotors para o CRM. O módulo não conhece a entidade Lead;
 * quem implementa (ver CrmLeadIntakeAdapter) aplica a regra oficial de entrada de leads.
 */
interface CrmLeadIntake {
    fun intake(command: LeadIntakeCommand): IntakeResult
}
