package com.astro.leadflow.shared.modules.integrations.webmotors

import com.astro.leadflow.shared.modules.integrations.webmotors.domain.AcceptMode
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.InboxStatus
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WmCredentials
import com.astro.leadflow.shared.modules.integrations.webmotors.service.AccountRuntime
import com.astro.leadflow.shared.modules.integrations.webmotors.service.InboxEntry
import java.time.LocalDateTime
import java.util.UUID

object WebmotorsFixtures {
    val accountId: UUID = UUID.randomUUID()
    val orgId: UUID = UUID.randomUUID()

    fun runtime(
        mode: AcceptMode = AcceptMode.MANUAL,
        welcome: Boolean = false,
        storeId: String? = null
    ) = AccountRuntime(
        id = accountId, organizationId = orgId, storeId = storeId,
        acceptMode = mode, welcomeMessageEnabled = welcome,
        credentials = WmCredentials(accountId, "cid", "secret", "user", "pass")
    )

    fun entry(
        status: InboxStatus = InboxStatus.PENDING,
        externalId: String = "460046",
        attempts: Int = 0
    ) = InboxEntry(
        id = UUID.randomUUID(), organizationId = orgId, accountId = accountId,
        externalId = externalId, leadType = 1, status = status,
        message = "Tenho interesse", channel = "Site", typeDescription = "Proposta",
        remoteStatus = "Não respondido", remoteCreatedAt = "2026-01-10T10:00:00",
        adId = "9988", adBrand = "Hyundai", adModel = "HB20", adVersion = "1.6 COMFORT",
        adYear = 2018, adPlate = "ABC-1234", adPrice = java.math.BigDecimal("55000.00"),
        refusalCode = null, refusalMessage = null, leadId = null, attempts = attempts,
        lastError = null, receivedAt = LocalDateTime.now(), decidedAt = null
    )

    /** Formato inferido da collection Postman (listagem). */
    const val LIST_JSON = """
        {
          "paginaAtual": 1, "totalItens": 2, "totalPaginas": 1,
          "mensagens": [
            {
              "id": 460046, "dataMensagem": "2026-01-10T10:00:00", "status": "Não respondido",
              "statusLead": 2, "codigoMotivoFinalizacao": 0, "midia": "Site",
              "tipoLead": 1, "subTipoLead": 0, "tipoLeadDesc": "Proposta", "mensagem": "Tenho interesse",
              "loja": {"id": 77, "nomeFantasia": "Loja Teste"},
              "anuncio": {"id": 9988, "marca": "Hyundai", "modelo": "HB20", "versao": "1.6 COMFORT",
                          "anoModelo": 2018, "anoFabricacao": 2018, "valor": 55000.00,
                          "placa": "ABC-1234", "tipoVeiculo": "Carro", "blindado": false, "campoNovo": "x"},
              "vendedor": {"id": 5, "nome": "Vendedor Fulano"}
            }
          ]
        }
    """
}
