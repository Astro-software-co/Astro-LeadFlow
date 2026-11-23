package com.astro.leadflow.shared.modules.ingestion.service

import com.astro.leadflow.shared.exception.ExternalServiceException
import com.astro.leadflow.shared.modules.communication.meta.MetaApiClient
import com.astro.leadflow.shared.modules.communication.service.MessageDeliveryFailureLogger
import com.astro.leadflow.shared.modules.communication.service.MessageSenderService
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.intake.LeadIntakeCommand
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.ingestion.dto.ScrapedLeadDTO
import com.astro.leadflow.shared.modules.ingestion.infra.SqsProducer
import com.astro.leadflow.shared.modules.saas.service.UsageLimitService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Transações reais (sem a transação do próprio teste): a falha de cota ou da Meta dentro de
 * `sendTemplate` não pode desfazer o lead já gravado por quem chamou.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(LeadIngestionService::class, MessageSenderService::class, MessageDeliveryFailureLogger::class, NormalizationService::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:ingesttx;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "meta.api.mode=mock",
        "app.aws.enabled=false"
    ]
)
class LeadIngestionTransactionTest {

    @Autowired lateinit var service: LeadIngestionService
    @Autowired lateinit var leads: LeadRepository
    @Autowired lateinit var orgs: OrganizationRepository

    @MockBean lateinit var metaApiClient: MetaApiClient
    @MockBean lateinit var usageLimitService: UsageLimitService
    @MockBean lateinit var sqsProducer: SqsProducer

    private lateinit var org: Organization

    @BeforeEach
    fun setup() {
        org = orgs.save(Organization(name = "Loja TX", slug = "loja-tx"))
    }

    @AfterEach
    fun cleanup() {
        leads.deleteAll()
        orgs.deleteAll()
    }

    private fun dto() = ScrapedLeadDTO(
        organizationId = org.id, organizationSlug = "loja-tx", name = "Cliente Teste",
        phoneNumber = "11999998888", source = "SCRAPER", marketplaceType = "WEBMOTORS",
        vehicleInterest = "Carro de teste"
    )

    @Test
    fun `cota excedida nao desfaz o lead`() {
        `when`(usageLimitService.canSendMessage(org.id, "WEBMOTORS")).thenReturn(false)

        service.ingestFromWebhook(dto())

        assertEquals(1, leads.findAllByOrganizationId(org.id).size)
    }

    @Test
    fun `falha da Meta nao desfaz o lead`() {
        `when`(usageLimitService.canSendMessage(org.id, "WEBMOTORS")).thenReturn(true)
        `when`(metaApiClient.sendTemplate(anyString(), anyString(), anyString(), anyString(), anyString(), anyList()))
            .thenThrow(ExternalServiceException("Meta/WhatsApp", "HTTP 500"))

        service.ingestFromWebhook(dto())

        assertEquals(1, leads.findAllByOrganizationId(org.id).size)
    }

    @Test
    fun `entrada por comando com falha da Meta mantem o lead`() {
        `when`(usageLimitService.canSendMessage(org.id, "WEBMOTORS")).thenReturn(true)
        `when`(metaApiClient.sendTemplate(anyString(), anyString(), anyString(), anyString(), anyString(), anyList()))
            .thenThrow(ExternalServiceException("Meta/WhatsApp", "HTTP 500"))

        service.ingestResolved(
            org,
            LeadIntakeCommand(
                organizationId = org.id, name = "Cliente", phone = "+5511999998888", email = null,
                origin = "WEBMOTORS", externalEventId = "1"
            )
        )

        assertEquals(1, leads.findAllByOrganizationId(org.id).size)
    }
}
