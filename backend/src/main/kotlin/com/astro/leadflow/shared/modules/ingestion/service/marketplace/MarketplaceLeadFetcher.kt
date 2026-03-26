package com.astro.leadflow.shared.modules.ingestion.service.marketplace

import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceLeadEvent
import com.astro.leadflow.shared.modules.ingestion.domain.RawMarketplaceLead
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredential
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import java.time.Instant

interface MarketplaceLeadFetcher {

    /** Busca os dados completos de um lead a partir da notificação webhook */
    fun fetchLeadDetail(event: MarketplaceLeadEvent, credential: MarketplaceCredential): RawMarketplaceLead

    /** Lista leads recebidos desde [since] — usado no job de reconciliação diária.
     *  Retorna lista vazia para marketplaces que não suportam listagem. */
    fun fetchLeadsSince(credential: MarketplaceCredential, since: Instant): List<RawMarketplaceLead>

    fun supports(type: MarketplaceType): Boolean
}
