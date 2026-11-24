package com.astro.leadflow.shared.modules.ingestion.service

import com.astro.leadflow.shared.modules.ingestion.domain.RawMarketplaceLead
import com.astro.leadflow.shared.modules.ingestion.dto.ScrapedLeadDTO
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import org.springframework.stereotype.Service

@Service
class LeadNormalizationService {

    /**
     * Converte um [RawMarketplaceLead] + org para o DTO canônico [ScrapedLeadDTO].
     * Campos nullable no raw são tratados graciosamente — nunca lança NPE.
     */
    fun normalize(raw: RawMarketplaceLead, organization: Organization): ScrapedLeadDTO {
        val phone = raw.buyerPhone?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Lead de marketplace sem telefone — descartado")

        val vehicleInterest = buildVehicleInterest(raw)
        val source = sourceFor(raw.marketplaceType)

        return ScrapedLeadDTO(
            organizationId = organization.id,
            organizationSlug = organization.slug,
            name = raw.buyerName ?: "",
            phoneNumber = phone,
            email = raw.buyerEmail,
            source = source,
            marketplaceType = raw.marketplaceType.name,
            vehicleInterest = vehicleInterest,
            rawData = emptyMap()
        )
    }

    private fun buildVehicleInterest(raw: RawMarketplaceLead): String {
        val parts = listOfNotNull(
            raw.vehicleBrand?.takeIf { it.isNotBlank() },
            raw.vehicleModel?.takeIf { it.isNotBlank() },
            raw.vehicleYear?.toString()
        )
        return parts.joinToString(" ").ifBlank { raw.contactType ?: "" }
    }

    private fun sourceFor(type: MarketplaceType): String = "MARKETPLACE_${type.name}"
}
