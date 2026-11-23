package com.astro.leadflow.shared.modules.crm.intake

import java.time.LocalDateTime
import java.util.UUID

/**
 * Comando único de entrada de lead no CRM (marketplace, scraper, Webmotors). Já carrega os dados
 * de anúncio/evento que o modelo Card + Interesses (phase G4) usa; hoje só parte deles é gravada.
 */
data class LeadIntakeCommand(
    val organizationId: UUID,
    val name: String?,
    /** Telefone como recebido; normalizado na entrada. Nulo ainda não é aceito (phase G9). */
    val phone: String?,
    val email: String?,
    /** Marketplace de origem (`MarketplaceType`), ex.: `WEBMOTORS`. */
    val origin: String,
    /** Id do evento no marketplace (idempotência). Nulo quando a fonte não tem (scraper). */
    val externalEventId: String?,
    val listingId: String? = null,
    val listingUrl: String? = null,
    val vehicleSnapshot: VehicleSnapshot? = null,
    /** Texto livre do veículo de interesse; se ausente, derivado do snapshot. */
    val vehicleInterest: String? = null,
    val contactType: String? = null,
    /** Texto gravado em `Lead.notes` (mensagem do cliente e resumo da origem). */
    val message: String? = null,
    val receivedAt: LocalDateTime = LocalDateTime.now(),
    /** Envia a mensagem de abordagem (M1). Sem ela o lead nasce em `WAITING_ATTENDANCE`. */
    val sendWelcome: Boolean = true
)
