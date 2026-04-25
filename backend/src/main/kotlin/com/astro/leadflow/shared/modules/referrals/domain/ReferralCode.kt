package com.astro.leadflow.shared.modules.referrals.domain

data class ReferralCode(val value: String) {
    init {
        require(value.isNotBlank()) { "Referral code must not be blank" }
    }

    companion object {
        fun of(raw: String) = ReferralCode(raw.trim().uppercase())
    }
}
