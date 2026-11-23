package com.astro.leadflow.shared.modules.chatbot.qualification

import com.astro.leadflow.shared.modules.crm.domain.InterestType
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.iam.domain.Organization
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class LeadQualificationServiceTest {
    private val repository = mockk<LeadRepository>()
    private val service = LeadQualificationService(repository)
    private val organization = Organization(name = "Astro", slug = "astro")

    private fun newLead() = Lead(organization = organization, phoneNumber = "+5511999999999", origin = "WHATSAPP_INBOUND")

    init {
        every { repository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `maps slots into lead and reports qualified`() {
        val lead = newLead()

        val qualified = service.apply(
            lead,
            mapOf("vehicle" to "Civic 2020", "paymentType" to "FINANCING", "timeline" to "IMEDIATO", "budget" to "80000")
        )

        assertTrue(qualified)
        assertEquals("Civic 2020", lead.vehicleInterest)
        assertEquals(InterestType.FINANCING, lead.interestType)
        assertEquals("alta", lead.urgency)
        assertEquals(BigDecimal("80000"), lead.budget)
    }

    @Test
    fun `is not qualified until all required slots exist`() {
        assertFalse(service.apply(newLead(), mapOf("vehicle" to "Civic")))
    }

    @Test
    fun `never overwrites existing data and keeps divergent interest as a single note`() {
        val lead = newLead().apply {
            vehicleInterest = "Corolla"
            interestType = InterestType.CASH_PURCHASE
        }
        val slots = mapOf("vehicle" to "Civic", "paymentType" to "FINANCING")

        service.apply(lead, slots)
        service.apply(lead, slots)

        assertEquals("Corolla", lead.vehicleInterest)
        assertEquals(InterestType.CASH_PURCHASE, lead.interestType)
        assertEquals("[Bot] Interesse informado no chat: Civic", lead.notes)
    }

    @Test
    fun `ignores invalid budget and unknown enum values`() {
        val lead = newLead()

        service.apply(lead, mapOf("budget" to "-5", "paymentType" to "BARTER"))

        assertEquals(null, lead.budget)
        assertEquals(null, lead.interestType)
    }
}
