package com.astro.leadflow.shared.modules.ingestion.service

import com.astro.leadflow.shared.exception.BusinessException
import org.springframework.stereotype.Service

@Service
class NormalizationService {

    fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }

        val normalized = when {
            digits.startsWith("55") && digits.length >= 12 -> "+$digits"
            digits.length == 11 -> "+55$digits"
            digits.length == 10 -> "+55$digits"
            else -> throw BusinessException("Número de telefone inválido: $raw")
        }

        if (!isValidBrazilianPhone(normalized))
            throw BusinessException("Número de telefone inválido: $raw")

        return normalized
    }

    fun normalizeMarketplace(marketplace: String): String =
        marketplace.uppercase().trim().replace(" ", "_")

    private fun isValidBrazilianPhone(phone: String): Boolean {
        val regex = Regex("""^\+55[1-9]{2}[6-9][0-9]{7,8}$""")
        return phone.matches(regex)
    }
}