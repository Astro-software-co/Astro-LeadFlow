package com.astro.leadflow.shared.modules.saas.domain

import java.time.LocalDate

enum class BillingCycle(
    val label: String,
    val sortOrder: Int,
) {
    WEEKLY("Semanal", 0),
    MONTHLY("Mensal", 1),
    SEMIANNUALLY("Semestral", 2),
    ANNUALLY("Anual", 3),
    ;

    fun nextRenewalFrom(baseDate: LocalDate): LocalDate =
        when (this) {
            WEEKLY -> baseDate.plusWeeks(1)
            MONTHLY -> baseDate.plusMonths(1)
            SEMIANNUALLY -> baseDate.plusMonths(6)
            ANNUALLY -> baseDate.plusYears(1)
        }

    companion object {
        fun fromApiValue(value: String?): BillingCycle =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
    }
}
