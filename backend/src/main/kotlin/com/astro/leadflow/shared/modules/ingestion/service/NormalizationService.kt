package com.astro.leadflow.modules.ingestion.service

import com.astro.leadflow.shared.exception.BusinessException
import org.springframework.stereotype.Service

@Service
class NormalizationService {

    /**
     * Padroniza número de telefone para formato internacional +55XXXXXXXXXXX.
     * Aceita formatos: (11) 98765-4321, 11987654321, 5511987654321, +5511987654321
     */
    fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }

        val normalized = when {
            digits.startsWith("55") && digits.length >= 12 -> "+$digits"
            digits.length == 11 -> "+55$digits"
            digits.length == 10 -> "+55$digits"
            else -> throw BusinessException("Número de telefone inválido: $raw")
        }

        if (!isValidBrazilianPhone(normalized)) {
            throw BusinessException("Número de telefone inválido: $raw")
        }

        return normalized
    }

    /**
     * Normaliza nome do marketplace para uppercase consistente.
     */
    fun normalizeMarketplace(marketplace: String): String =
        marketplace.uppercase().trim().replace(" ", "_")

    private fun isValidBrazilianPhone(phone: String): Boolean {
        // +55 + DDD (2 dígitos) + número (8 ou 9 dígitos)
        val regex = Regex("""^\+55[1-9]{2}[6-9][0-9]{7,8}$""")
        return phone.matches(regex)
    }
}