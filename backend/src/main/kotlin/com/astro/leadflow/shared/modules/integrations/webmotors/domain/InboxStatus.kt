package com.astro.leadflow.shared.modules.integrations.webmotors.domain

enum class InboxStatus {
    /** Recebida da Webmotors, aguardando aceite/recusa. Ainda não é um Lead do CRM. */
    PENDING,

    /** Aceita na Webmotors; falta obter o contato e criar o Lead. */
    ACCEPTED,

    /** Aceita, mas o telefone ainda não está utilizável (ex.: mascarado). Será tentado de novo. */
    AWAITING_CONTACT,

    /** Virou Lead no CRM. */
    IMPORTED,

    REFUSED,

    /** Desistimos após esgotar as tentativas. Requer ação manual. */
    FAILED
}
