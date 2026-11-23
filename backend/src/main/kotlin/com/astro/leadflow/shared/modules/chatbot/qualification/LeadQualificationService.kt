package com.astro.leadflow.shared.modules.chatbot.qualification

import com.astro.leadflow.shared.modules.crm.domain.InterestType
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Copia os slots canônicos do bot (Lex ou mock) para os campos de qualificação do Lead.
 * Nunca sobrescreve dado já preenchido por humano ou marketplace.
 */
@Service
class LeadQualificationService(
    private val leadRepository: LeadRepository
) {
    /** Retorna true quando os três slots obrigatórios (veículo, pagamento, prazo) estão preenchidos. */
    @Transactional
    fun apply(lead: Lead, slots: Map<String, String>): Boolean {
        var changed = false

        val vehicle = slots[VEHICLE]?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_VEHICLE_LENGTH)
        if (vehicle != null) {
            if (lead.vehicleInterest.isNullOrBlank()) {
                lead.vehicleInterest = vehicle
                changed = true
            } else if (!lead.vehicleInterest.equals(vehicle, ignoreCase = true)) {
                changed = appendNote(lead, "Interesse informado no chat: $vehicle") || changed
            }
        }

        val interestType = slots[PAYMENT_TYPE]?.let { runCatching { InterestType.valueOf(it) }.getOrNull() }
        if (interestType != null && lead.interestType == null) {
            lead.interestType = interestType
            changed = true
        }

        val timeline = TIMELINES[slots[TIMELINE]]
        if (timeline != null && lead.purchaseForecast.isNullOrBlank()) {
            lead.purchaseForecast = timeline.forecast
            if (lead.urgency.isNullOrBlank()) lead.urgency = timeline.urgency
            changed = true
        }

        val budget = slots[BUDGET]?.let { parseBudget(it) }
        if (budget != null && lead.budget == null) {
            lead.budget = budget
            changed = true
        }

        if (changed) leadRepository.save(lead)

        return !lead.vehicleInterest.isNullOrBlank() &&
            lead.interestType != null &&
            !lead.purchaseForecast.isNullOrBlank()
    }

    private fun appendNote(lead: Lead, text: String): Boolean {
        val line = "[Bot] $text"
        val current = lead.notes.orEmpty()
        if (current.contains(line)) return false
        lead.notes = if (current.isBlank()) line else "$current\n$line"
        return true
    }

    private fun parseBudget(raw: String): BigDecimal? {
        val value = raw.trim().toBigDecimalOrNull() ?: return null
        return value.takeIf { it > BigDecimal.ZERO && it <= MAX_BUDGET }
    }

    private data class Timeline(val forecast: String, val urgency: String)

    companion object {
        const val VEHICLE = "vehicle"
        const val PAYMENT_TYPE = "paymentType"
        const val TIMELINE = "timeline"
        const val BUDGET = "budget"

        private const val MAX_VEHICLE_LENGTH = 200
        private val MAX_BUDGET = BigDecimal("100000000")

        private val TIMELINES = mapOf(
            "IMEDIATO" to Timeline("Imediato", "alta"),
            "30_DIAS" to Timeline("Até 30 dias", "media"),
            "90_DIAS_OU_MAIS" to Timeline("90 dias ou mais", "baixa")
        )
    }
}
