package com.astro.leadflow.shared.modules.integrations.webmotors.service

import com.astro.leadflow.shared.modules.integrations.webmotors.domain.AcceptMode

/** Campos nulos/vazios de credencial mantêm o valor já salvo (permite editar sem reenviar segredos). */
data class AccountCommand(
    val clientId: String? = null,
    val clientSecret: String? = null,
    val username: String? = null,
    val password: String? = null,
    val storeId: String? = null,
    val acceptMode: AcceptMode? = null,
    val welcomeMessageEnabled: Boolean? = null,
    val enabled: Boolean? = null
)
