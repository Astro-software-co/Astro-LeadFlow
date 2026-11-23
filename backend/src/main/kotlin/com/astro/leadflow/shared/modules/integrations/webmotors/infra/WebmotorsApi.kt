package com.astro.leadflow.shared.modules.integrations.webmotors.infra

import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmKeyValue
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmLeadPage
import com.fasterxml.jackson.databind.JsonNode

/**
 * Porta para a API de Leads da Webmotors. Isolada em interface para o restante do módulo
 * (sync, decisões) ser testável sem rede. Específica da Webmotors de propósito: outros
 * marketplaces terão suas próprias portas.
 */
interface WebmotorsApi {
    /** Faz login e valida as credenciais. Lança [WebmotorsApiException] se falhar. */
    fun login(credentials: WmCredentials)

    fun fetchPendingLeads(credentials: WmCredentials, page: Int? = null): WmLeadPage

    /** Detalhe cru: o formato de contato (nome/e-mail/telefone) ainda não está confirmado. */
    fun fetchLeadDetail(credentials: WmCredentials, leadId: String, leadType: Int): JsonNode

    fun accept(credentials: WmCredentials, leadId: String, leadType: Int)

    fun refuse(credentials: WmCredentials, leadId: String, leadType: Int, reasonCode: Int, message: String?)

    fun changeStatus(credentials: WmCredentials, leadId: String, leadType: Int, reasonCode: Int, message: String?)

    fun refusalReasons(credentials: WmCredentials, leadType: Int): List<WmKeyValue>

    /** Domínios ainda sem formato confirmado, expostos crus para descoberta na homologação. */
    fun leadTypes(credentials: WmCredentials): JsonNode

    fun channels(credentials: WmCredentials): JsonNode
}
