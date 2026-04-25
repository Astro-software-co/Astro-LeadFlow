package com.astro.leadflow.shared.modules.referrals.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

class ReferralControllerTest {

    @Test
    fun `dealership referral controller exposes exact prompt endpoints`() {
        val controllerMapping = ReferralController::class.java.getAnnotation(RequestMapping::class.java)
        val preAuthorize = ReferralController::class.java.getAnnotation(PreAuthorize::class.java)

        assertNotNull(controllerMapping)
        assertEquals(listOf("/api/referral"), controllerMapping.value.toList())
        assertEquals("hasRole('DEALERSHIP')", preAuthorize.value)

        assertEquals(
            listOf("/my-code"),
            ReferralController::class.java.getDeclaredMethod("getMyCode")
                .getAnnotation(GetMapping::class.java)
                .value
                .toList()
        )
        assertEquals(
            listOf("/my-referrals"),
            ReferralController::class.java.getDeclaredMethod("getMyReferrals")
                .getAnnotation(GetMapping::class.java)
                .value
                .toList()
        )
        assertEquals(
            listOf("/my-balance"),
            ReferralController::class.java.getDeclaredMethod("getMyBalance")
                .getAnnotation(GetMapping::class.java)
                .value
                .toList()
        )
    }

    @Test
    fun `admin referral controller exposes exact prompt summary endpoint`() {
        val controllerMapping = AdminReferralController::class.java.getAnnotation(RequestMapping::class.java)
        val preAuthorize = AdminReferralController::class.java.getAnnotation(PreAuthorize::class.java)
        val summaryMapping = AdminReferralController::class.java.getDeclaredMethod("getSummary")
            .getAnnotation(GetMapping::class.java)

        assertNotNull(controllerMapping)
        assertEquals(listOf("/api/admin/referral"), controllerMapping.value.toList())
        assertEquals("hasRole('ASTRO_ADMIN')", preAuthorize.value)
        assertEquals(listOf("/summary"), summaryMapping.value.toList())
    }
}
