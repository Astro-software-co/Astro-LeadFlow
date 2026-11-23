package com.astro.leadflow.shared.modules.integrations.webmotors.service

import com.astro.leadflow.shared.modules.integrations.webmotors.domain.AcceptMode
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WmCredentials
import java.util.UUID

/** Tudo que o sync precisa de uma conta, já decifrado e sem entidades JPA. */
data class AccountRuntime(
    val id: UUID,
    val organizationId: UUID,
    val storeId: String?,
    val acceptMode: AcceptMode,
    val welcomeMessageEnabled: Boolean,
    val credentials: WmCredentials
)
