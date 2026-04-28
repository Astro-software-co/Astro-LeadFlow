package com.astro.leadflow.shared.modules.monitoring.domain.enums

enum class WebhookOutcome {
    SUCCESS,
    SIGNATURE_FAIL,
    RATE_LIMITED,
    PROCESSING_ERROR,
    REPLAY_BLOCKED
}
