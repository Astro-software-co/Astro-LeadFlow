package com.astro.leadflow.shared.modules.integrations.webmotors.domain

enum class AcceptMode {
    /** O lojista aceita/recusa cada proposta pela tela. Opcional por loja. */
    MANUAL,

    /** O sistema aceita todas as propostas assim que chegam. Padrão (D11). */
    AUTO
}
