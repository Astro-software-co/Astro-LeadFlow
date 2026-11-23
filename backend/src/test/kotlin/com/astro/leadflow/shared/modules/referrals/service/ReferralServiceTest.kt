package com.astro.leadflow.shared.modules.referrals.service

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.modules.iam.service.UserReferralCodeService
import com.astro.leadflow.shared.modules.referrals.domain.CommissionRecord
import com.astro.leadflow.shared.modules.referrals.domain.CommissionRecordRepository
import com.astro.leadflow.shared.modules.referrals.domain.ReferralRelationship
import com.astro.leadflow.shared.modules.referrals.domain.ReferralRelationshipRepository
import com.astro.leadflow.shared.modules.referrals.domain.ReferralStatus
import com.astro.leadflow.shared.modules.saas.domain.BillingHistoryEntry
import com.astro.leadflow.shared.modules.saas.domain.Subscription
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class ReferralServiceTest {

    private lateinit var referralRepository: ReferralRelationshipRepository
    private lateinit var commissionRepository: CommissionRecordRepository
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var userReferralCodeService: UserReferralCodeService
    private lateinit var service: ReferralService

    private val orgId = UUID.randomUUID()
    private val referrerOrg = Organization(id = orgId, name = "Referrer Org", slug = "referrer-org")

    private fun makeRelationship(
        status: ReferralStatus = ReferralStatus.APPROVED,
        referredOrg: Organization = Organization(id = UUID.randomUUID(), name = "Ref ${UUID.randomUUID()}", slug = "ref")
    ) = ReferralRelationship(
        referrerOrganization = referrerOrg,
        referredOrganization = referredOrg,
        status = status
    )

    private fun makeBillingEntry(subscription: Subscription, amountBrl: BigDecimal): BillingHistoryEntry =
        BillingHistoryEntry(
            subscription = subscription,
            description = "Cobrança mensal",
            amountBrl = amountBrl,
            status = "PAID",
            billedAt = LocalDate.now(),
            issuedAt = LocalDate.now()
        )

    @BeforeEach
    fun setup() {
        referralRepository = mockk()
        commissionRepository = mockk()
        organizationRepository = mockk()
        userRepository = mockk(relaxed = true)
        userReferralCodeService = mockk(relaxed = true)
        service = ReferralService(
            referralRepository,
            commissionRepository,
            organizationRepository,
            userRepository,
            userReferralCodeService
        )
    }

    // ── Tier thresholds ───────────────────────────────────────────────────

    @Test
    fun `tier is BRONZE when approved referrals count is below 10`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 5
        assertEquals(ReferralTier.BRONZE, service.getTier(orgId))
    }

    @Test
    fun `tier is SILVER when approved referrals count is exactly 10`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 10
        assertEquals(ReferralTier.SILVER, service.getTier(orgId))
    }

    @Test
    fun `tier is SILVER when approved referrals count is between 10 and 19`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 15
        assertEquals(ReferralTier.SILVER, service.getTier(orgId))
    }

    @Test
    fun `tier is GOLD when approved referrals count is exactly 20`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 20
        assertEquals(ReferralTier.GOLD, service.getTier(orgId))
    }

    @Test
    fun `tier is GOLD when approved referrals count is between 20 and 34`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 28
        assertEquals(ReferralTier.GOLD, service.getTier(orgId))
    }

    @Test
    fun `tier is DIAMOND when approved referrals count is exactly 35`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 35
        assertEquals(ReferralTier.DIAMOND, service.getTier(orgId))
    }

    @Test
    fun `tier is DIAMOND when approved referrals count exceeds 35`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 50
        assertEquals(ReferralTier.DIAMOND, service.getTier(orgId))
    }

    @Test
    fun `tier is BRONZE when there are zero approved referrals`() {
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 0
        assertEquals(ReferralTier.BRONZE, service.getTier(orgId))
    }

    @Test
    fun `referral tier thresholds follow BRONZE less than 10, SILVER 10-19, GOLD 20-34, DIAMOND 35 plus`() {
        val cases = mapOf(
            0L to ReferralTier.BRONZE, 9L to ReferralTier.BRONZE,
            10L to ReferralTier.SILVER, 19L to ReferralTier.SILVER,
            20L to ReferralTier.GOLD, 34L to ReferralTier.GOLD,
            35L to ReferralTier.DIAMOND, 100L to ReferralTier.DIAMOND
        )
        cases.forEach { (count, expectedTier) ->
            every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns count
            assertEquals(expectedTier, service.getTier(orgId), "Expected $expectedTier for count=$count")
        }
    }

    // ── listReferrals ─────────────────────────────────────────────────────

    @Test
    fun `listReferrals returns all relationships for referrer organization`() {
        val relationships = listOf(makeRelationship(), makeRelationship(ReferralStatus.PENDING))
        every { referralRepository.findAllByReferrerOrganizationId(orgId) } returns relationships
        assertEquals(2, service.listReferrals(orgId).size)
    }

    @Test
    fun `listReferrals returns empty list when no referrals exist`() {
        every { referralRepository.findAllByReferrerOrganizationId(orgId) } returns emptyList()
        assertTrue(service.listReferrals(orgId).isEmpty())
    }

    // ── Commission on first payment ────────────────────────────────────────

    @Test
    fun `generateCommissionIfFirstPayment creates commission record for first paid event`() {
        val referredOrgId = UUID.randomUUID()
        val referredOrg = Organization(id = referredOrgId, name = "Referred", slug = "referred")
        val relationship = makeRelationship(ReferralStatus.APPROVED, referredOrg)
        val subscription = mockk<Subscription>(relaxed = true)
        val billingEntry = makeBillingEntry(subscription, BigDecimal("199.90"))

        every { referralRepository.findByReferredOrganizationId(referredOrgId) } returns Optional.of(relationship)
        every { commissionRepository.existsByReferralRelationshipId(relationship.id) } returns false
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 5
        every { commissionRepository.save(any()) } answers { firstArg() }

        service.generateCommissionIfFirstPayment(referredOrgId, billingEntry)

        val slot = slot<CommissionRecord>()
        verify { commissionRepository.save(capture(slot)) }
        assertEquals(BigDecimal("15.99"), slot.captured.amountBrl) // 199.90 * 0.08 = 15.992 → 15.99
        assertEquals(ReferralTier.BRONZE.name, slot.captured.tier)
    }

    @Test
    fun `generateCommissionIfFirstPayment does nothing when commission already exists`() {
        val referredOrgId = UUID.randomUUID()
        val referredOrg = Organization(id = referredOrgId, name = "Referred", slug = "referred")
        val relationship = makeRelationship(ReferralStatus.APPROVED, referredOrg)
        val subscription = mockk<Subscription>(relaxed = true)
        val billingEntry = makeBillingEntry(subscription, BigDecimal("199.90"))

        every { referralRepository.findByReferredOrganizationId(referredOrgId) } returns Optional.of(relationship)
        every { commissionRepository.existsByReferralRelationshipId(relationship.id) } returns true

        service.generateCommissionIfFirstPayment(referredOrgId, billingEntry)

        verify(exactly = 0) { commissionRepository.save(any()) }
    }

    @Test
    fun `generateCommissionIfFirstPayment does nothing when referred org has no referral relationship`() {
        val referredOrgId = UUID.randomUUID()
        val subscription = mockk<Subscription>(relaxed = true)
        val billingEntry = makeBillingEntry(subscription, BigDecimal("199.90"))

        every { referralRepository.findByReferredOrganizationId(referredOrgId) } returns Optional.empty()

        service.generateCommissionIfFirstPayment(referredOrgId, billingEntry)

        verify(exactly = 0) { commissionRepository.save(any()) }
    }

    @Test
    fun `generateCommissionIfFirstPayment applies SILVER rate for referrer with 12 approved`() {
        val referredOrgId = UUID.randomUUID()
        val referredOrg = Organization(id = referredOrgId, name = "Referred", slug = "referred")
        val relationship = makeRelationship(ReferralStatus.APPROVED, referredOrg)
        val subscription = mockk<Subscription>(relaxed = true)
        val billingEntry = makeBillingEntry(subscription, BigDecimal("100.00"))

        every { referralRepository.findByReferredOrganizationId(referredOrgId) } returns Optional.of(relationship)
        every { commissionRepository.existsByReferralRelationshipId(relationship.id) } returns false
        every { referralRepository.countByReferrerOrganizationIdAndStatus(orgId, ReferralStatus.APPROVED) } returns 12
        every { commissionRepository.save(any()) } answers { firstArg() }

        service.generateCommissionIfFirstPayment(referredOrgId, billingEntry)

        val slot = slot<CommissionRecord>()
        verify { commissionRepository.save(capture(slot)) }
        assertEquals(BigDecimal("12.00"), slot.captured.amountBrl) // 100.00 * 0.12
        assertEquals(ReferralTier.SILVER.name, slot.captured.tier)
    }
}
