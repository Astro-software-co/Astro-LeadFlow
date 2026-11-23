package com.astro.leadflow.shared.modules.integrations.webmotors.service

data class SyncReport(
    val fetched: Int = 0,
    val newProposals: Int = 0,
    val accepted: Int = 0,
    val retried: Int = 0,
    val skipped: Int = 0,
    val error: String? = null
)
