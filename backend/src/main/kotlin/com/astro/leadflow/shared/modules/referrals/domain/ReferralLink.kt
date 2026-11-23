package com.astro.leadflow.shared.modules.referrals.domain

data class ReferralLink(val url: String) {
    companion object {
        private const val ONBOARDING_BASE = "https://app.astroleadflow.com/onboarding"

        fun fromCode(code: String) = ReferralLink("$ONBOARDING_BASE?ref=$code")
    }
}
