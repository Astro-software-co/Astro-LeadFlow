package com.astro.leadflow.shared.modules.onboarding.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class OnboardingStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    PENDING;

    fun isPendingApproval(): Boolean = this == PENDING_APPROVAL || this == PENDING

    fun canonical(): OnboardingStatus = if (this == PENDING) PENDING_APPROVAL else this

    @JsonValue
    fun toJson(): String = canonical().name

    companion object {
        @JvmStatic
        @JsonCreator
        fun from(value: String): OnboardingStatus =
            when (value.trim().uppercase()) {
                "PENDING", "PENDING_APPROVAL" -> PENDING_APPROVAL
                "APPROVED" -> APPROVED
                "REJECTED" -> REJECTED
                else -> throw IllegalArgumentException("Unknown onboarding status: $value")
            }
    }
}
