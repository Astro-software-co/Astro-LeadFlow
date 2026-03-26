package com.astro.leadflow.shared.modules.ingestion.domain

object WebhookEventStatus {
    const val RECEIVED = "RECEIVED"
    const val QUEUED = "QUEUED"
    const val PROCESSING = "PROCESSING"
    const val INGESTED = "INGESTED"
    const val FAILED = "FAILED"
    const val QUOTA_EXCEEDED = "QUOTA_EXCEEDED"
    const val DUPLICATE = "DUPLICATE"
    const val IGNORED = "IGNORED"
}
