package com.astro.leadflow.shared.modules.referrals.service

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.referrals.domain.ReferralRelationship
import com.astro.leadflow.shared.modules.referrals.domain.ReferralRelationshipRepository
import com.astro.leadflow.shared.modules.referrals.domain.ReferralStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ReferralServiceTest {

    private lateinit var referralRepository: ReferralRelationshipRepository
    private lateinit var service: ReferralService

    private val orgId = UUID.randomUUID()
    private val referrerOrg = Organization(id = orgId, name = "Referrer Org", slug = "referrer-org")

    private fun makeRelationship(status: ReferralStatus = ReferralStatus.APPROVED): ReferralRelationship {
        val referredOrg = Organization(id = UUID.randomUUID(), name = "Referred Org ${UUID.randomUUID()}", slug = "referred-${UUID.randomUUID()}")
        return ReferralRelationship(
            referrerOrganization = referrerOrg,
            referredOrganization = referredOrg,
            status = status
        )
    }

    @BeforeEach
    fun setup() {
        referralRepository = mockk()
        service = ReferralService(referralRepository)
    }

    // ── Tier thresholds ───────────────────────────────────────────────────

    @Test
    fun `tier is BRONZE when approved referrals count is below 10`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 5

        val tier = service.getTier(orgId)

        assertEquals(ReferralTier.BRONZE, tier)
    }

    @Test
    fun `tier is SILVER when approved referrals count is exactly 10`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 10

        val tier = service.getTier(orgId)

        assertEquals(ReferralTier.SILVER, tier)
    }

    @Test
    fun `tier is SILVER when approved referrals count is between 10 and 19`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 15

        val tier = service.getTier(orgId)

        assertEquals(ReferralTier.SILVER, tier)
    }

    @Test
    fun `tier is GOLD when approved referrals count is exactly 20`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 20

        val tier = service.getTier(orgId)

        assertEquals(ReferralTier.GOLD, tier)
    }

    @Test
    fun `tier is GOLD when approved referrals count is between 20 and 34`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 28

        val tier = service.getTier(orgId)

        assertEquals(ReferralTier.GOLD, tier)
    }

    @Test
    fun `tier is DIAMOND when approved referrals count is exactly 35`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 35

        val tier = service.getTier(orgId)

        assertEquals(ReferralTier.DIAMOND, tier)
    }

    @Test
    fun `tier is DIAMOND when approved referrals count exceeds 35`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 50

        val tier = service.getTier(orgId)

        assertEquals(ReferralTier.DIAMOND, tier)
    }

    @Test
    fun `tier is BRONZE when there are zero approved referrals`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 0

        val tier = service.getTier(orgId)

        assertEquals(ReferralTier.BRONZE, tier)
    }

    // ── listReferrals ─────────────────────────────────────────────────────

    @Test
    fun `listReferrals returns all relationships for referrer organization`() {
        val relationships = listOf(makeRelationship(), makeRelationship(ReferralStatus.PENDING))
        every { referralRepository.findAllByReferrerOrganizationId(orgId) } returns relationships

        val result = service.listReferrals(orgId)

        assertEquals(2, result.size)
    }

    @Test
    fun `listReferrals returns empty list when no referrals exist`() {
        every { referralRepository.findAllByReferrerOrganizationId(orgId) } returns emptyList()

        val result = service.listReferrals(orgId)

        assertTrue(result.isEmpty())
    }

    // ── Self-attribution guard ─────────────────────────────────────────────

    @Test
    fun `referral tier thresholds follow BRONZE less than 10, SILVER 10-19, GOLD 20-34, DIAMOND 35 plus`() {
        val cases = mapOf(
            0L to ReferralTier.BRONZE,
            9L to ReferralTier.BRONZE,
            10L to ReferralTier.SILVER,
            19L to ReferralTier.SILVER,
            20L to ReferralTier.GOLD,
            34L to ReferralTier.GOLD,
            35L to ReferralTier.DIAMOND,
            100L to ReferralTier.DIAMOND
        )

        cases.forEach { (count, expectedTier) ->
            every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns count
            val actual = service.getTier(orgId)
            assertEquals(expectedTier, actual, "Expected $expectedTier for count=$count but got $actual")
        }
    }
}
