package com.astro.leadflow.shared.modules.ingestion.service

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.ingestion.domain.RawMarketplaceLead
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class LeadNormalizationServiceTest {

    private lateinit var service: LeadNormalizationService

    private val organization = Organization(
        id = UUID.randomUUID(),
        name = "Concessionária Test",
        slug = "concessionaria-test"
    )

    private fun buildRaw(
        type: MarketplaceType = MarketplaceType.MERCADO_LIVRE,
        phone: String? = "11999998888",
        name: String? = "João Silva",
        email: String? = "joao@email.com",
        brand: String? = "Toyota",
        model: String? = "Corolla",
        year: Int? = 2023,
        contactType: String? = "Proposta"
    ) = RawMarketplaceLead(
        externalId = "lead-001",
        marketplaceType = type,
        buyerName = name,
        buyerEmail = email,
        buyerPhone = phone,
        vehicleBrand = brand,
        vehicleModel = model,
        vehicleYear = year,
        listingId = "MLB-12345",
        listingUrl = null,
        contactType = contactType,
        receivedAt = Instant.now(),
        rawPayload = "{}"
    )

    @BeforeEach
    fun setup() {
        service = LeadNormalizationService()
    }

    @Test
    fun `quando todos os campos presentes deve mapear completamente para ScrapedLeadDTO`() {
        val raw = buildRaw()

        val dto = service.normalize(raw, organization)

        assertEquals(organization.id, dto.organizationId)
        assertEquals("concessionaria-test", dto.organizationSlug)
        assertEquals("João Silva", dto.name)
        assertEquals("11999998888", dto.phoneNumber)
        assertEquals("joao@email.com", dto.email)
        assertEquals("Toyota Corolla 2023", dto.vehicleInterest)
        assertEquals("MARKETPLACE_MERCADO_LIVRE", dto.source)
        assertEquals("MERCADO_LIVRE", dto.marketplaceType)
    }

    @Test
    fun `source deve ser MARKETPLACE_MERCADO_LIVRE para leads ML`() {
        val raw = buildRaw(type = MarketplaceType.MERCADO_LIVRE, brand = null, model = null, year = null, contactType = "whatsapp")

        val dto = service.normalize(raw, organization)

        assertEquals("MARKETPLACE_MERCADO_LIVRE", dto.source)
    }

    @Test
    fun `quando vehicleYear nulo deve omitir ano do vehicleInterest`() {
        val raw = buildRaw(year = null)

        val dto = service.normalize(raw, organization)

        assertEquals("Toyota Corolla", dto.vehicleInterest)
    }

    @Test
    fun `quando vehicleBrand e vehicleModel nulos deve usar contactType como vehicleInterest`() {
        val raw = buildRaw(brand = null, model = null, year = null, contactType = "whatsapp")

        val dto = service.normalize(raw, organization)

        assertEquals("whatsapp", dto.vehicleInterest)
    }

    @Test
    fun `quando todos os campos de veiculo e contactType nulos vehicleInterest deve ser string vazia`() {
        val raw = buildRaw(brand = null, model = null, year = null, contactType = null)

        val dto = service.normalize(raw, organization)

        assertEquals("", dto.vehicleInterest)
    }

    @Test
    fun `quando buyerPhone ausente deve lançar exceção`() {
        val raw = buildRaw(phone = null)

        assertThrows<IllegalArgumentException> {
            service.normalize(raw, organization)
        }
    }

    @Test
    fun `quando buyerPhone em branco deve lançar exceção`() {
        val raw = buildRaw(phone = "   ")

        assertThrows<IllegalArgumentException> {
            service.normalize(raw, organization)
        }
    }

    @Test
    fun `quando buyerEmail ausente deve criar DTO com email nulo`() {
        val raw = buildRaw(email = null)

        val dto = service.normalize(raw, organization)

        assertNull(dto.email)
    }

    @Test
    fun `quando buyerName nulo deve usar string vazia no name`() {
        val raw = buildRaw(name = null)

        val dto = service.normalize(raw, organization)

        assertEquals("", dto.name)
    }

    @Test
    fun `campos nullable ausentes não devem lançar NPE`() {
        val raw = RawMarketplaceLead(
            externalId = "lead-minimal",
            marketplaceType = MarketplaceType.MERCADO_LIVRE,
            buyerName = null,
            buyerEmail = null,
            buyerPhone = "11999990000",
            vehicleBrand = null,
            vehicleModel = null,
            vehicleYear = null,
            listingId = null,
            listingUrl = null,
            contactType = null,
            receivedAt = Instant.now(),
            rawPayload = "{}"
        )

        val dto = service.normalize(raw, organization)

        assertNotNull(dto)
        assertEquals("11999990000", dto.phoneNumber)
    }
}
