package com.astro.leadflow.shared.modules.integrations.webmotors.service

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.modules.crm.intake.LeadIntakeCommand
import com.astro.leadflow.shared.modules.crm.intake.VehicleSnapshot
import com.astro.leadflow.shared.modules.ingestion.service.NormalizationService
import com.astro.leadflow.shared.modules.integrations.webmotors.config.WebmotorsProperties
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.InboxStatus
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApi
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApiException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Transforma uma proposta ACEITA em Lead do CRM: busca o contato no detalhe, valida o telefone
 * e entrega pela porta [CrmLeadIntake]. Se o contato ainda não estiver utilizável (a collection
 * sugere telefone mascarado), a proposta fica AWAITING_CONTACT e é tentada de novo no próximo ciclo.
 */
@Service
class WebmotorsLeadImporter(
    private val api: WebmotorsApi,
    private val inbox: WebmotorsInboxService,
    private val accounts: WebmotorsAccountService,
    private val intake: CrmLeadIntake,
    private val normalization: NormalizationService,
    private val props: WebmotorsProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun tryImport(entryId: UUID): InboxEntry {
        val entry = inbox.get(entryId)
        if (entry.status !in IMPORTABLE) return entry

        val runtime = accounts.runtimeOf(entry.accountId)

        val detail = try {
            api.fetchLeadDetail(runtime.credentials, entry.externalId, entry.leadType)
        } catch (e: WebmotorsApiException) {
            log.warn("Detalhe do lead Webmotors {} indisponível: {}", entry.externalId, e.message)
            return inbox.markContactIssue(entry.id, e.message ?: "detalhe indisponível", props.maxContactAttempts)
        }

        val contact = WebmotorsContactExtractor.extract(detail)
        val phone = try {
            normalization.normalizePhone(contact.phone ?: "")
        } catch (e: BusinessException) {
            // Não loga o número: pode ser dado pessoal.
            log.info("Lead Webmotors {} sem telefone utilizável; nova tentativa no próximo ciclo.", entry.externalId)
            return inbox.markContactIssue(entry.id, "Telefone indisponível ou mascarado.", props.maxContactAttempts)
        }

        val result = intake.intake(
            LeadIntakeCommand(
                organizationId = entry.organizationId,
                name = contact.name,
                phone = phone,
                email = contact.email,
                origin = ORIGIN,
                externalEventId = entry.externalId,
                listingId = entry.adId,
                vehicleSnapshot = VehicleSnapshot(
                    brand = entry.adBrand, model = entry.adModel, version = entry.adVersion,
                    year = entry.adYear, price = entry.adPrice, plate = entry.adPlate
                ),
                contactType = entry.typeDescription,
                message = notes(entry),
                receivedAt = entry.receivedAt,
                sendWelcome = runtime.welcomeMessageEnabled
            )
        )
        return inbox.markImported(entry.id, result.leadId)
    }

    private fun notes(e: InboxEntry): String = buildString {
        append("Webmotors #${e.externalId}")
        e.typeDescription?.let { append(" · $it") }
        e.channel?.let { append(" · $it") }
        e.message?.takeIf { it.isNotBlank() }?.let { append("\nMensagem: $it") }
        val ad = listOfNotNull(
            e.adId?.let { "anúncio $it" },
            e.adPlate?.let { "placa $it" },
            e.adPrice?.let { "valor R$ $it" }
        )
        if (ad.isNotEmpty()) append("\nAnúncio: ${ad.joinToString(", ")}")
    }

    companion object {
        const val ORIGIN = "WEBMOTORS"
        private val IMPORTABLE = setOf(InboxStatus.ACCEPTED, InboxStatus.AWAITING_CONTACT)
    }
}
