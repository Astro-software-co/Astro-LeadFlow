package com.astro.leadflow.shared.modules.iam.service

import com.astro.leadflow.shared.modules.iam.domain.User
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
class UserReferralCodeService(
    private val userRepository: UserRepository
) {
    private val random = SecureRandom()

    @Transactional
    fun ensureReferralCode(user: User): String {
        val current = user.referralCode
        if (current != null && current.matches(REFERRAL_CODE_REGEX)) {
            return current
        }

        val code = generateUniqueReferralCode()
        user.referralCode = code
        userRepository.save(user)
        return code
    }

    fun generateUniqueReferralCode(): String {
        repeat(MAX_ATTEMPTS) {
            val code = (100000 + random.nextInt(900000)).toString()
            if (!userRepository.existsByReferralCode(code)) {
                return code
            }
        }

        throw IllegalStateException("Unable to generate a unique referral code")
    }

    private companion object {
        private const val MAX_ATTEMPTS = 100
        private val REFERRAL_CODE_REGEX = Regex("^\\d{6}$")
    }
}
