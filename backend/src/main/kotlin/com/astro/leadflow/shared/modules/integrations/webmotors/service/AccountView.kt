package com.astro.leadflow.shared.modules.integrations.webmotors.service

import com.astro.leadflow.shared.modules.integrations.webmotors.domain.AcceptMode
import java.time.LocalDateTime
import java.util.UUID

/** Visão segura (sem segredos) da conta. */
data class AccountView(
    val id: UUID,
    val storeId: String?,
    val clientIdHint: String,
    val acceptMode: AcceptMode,
    val welcomeMessageEnabled: Boolean,
    val enabled: Boolean,
    val lastSyncAt: LocalDateTime?,
    val lastSyncOk: Boolean?,
    val lastSyncError: String?
)
