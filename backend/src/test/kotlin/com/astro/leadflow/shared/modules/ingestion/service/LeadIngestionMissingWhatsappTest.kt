package com.astro.leadflow.shared.modules.ingestion.service

import com.astro.leadflow.shared.modules.communication.meta.MetaApiClient
import com.astro.leadflow.shared.modules.communication.service.MessageDeliveryFailureLogger
import com.astro.leadflow.shared.modules.communication.service.MessageSenderService
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.intake.LeadIntakeCommand
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.ingestion.infra.SqsProducer
import com.astro.leadflow.shared.modules.saas.service.UsageLimitService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** G1-03: organização sem credencial do WhatsApp no modo real não pode perder o lead. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(LeadIngestionService::class, MessageSenderService::class, MessageDeliveryFailureLogger::class, NormalizationService::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:ingestwa;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "meta.api.mode=real",
        "meta.api.access-token=",
        "meta.api.phone-number-id="
    ]
)
class LeadIngestionMissingWhatsappTest {

    @Autowired lateinit var service: LeadIngestionService
    @Autowired lateinit var leads: LeadRepository
    @Autowired lateinit var orgs: OrganizationRepository

    @MockBean lateinit var metaApiClient: MetaApiClient
    @MockBean lateinit var usageLimitService: UsageLimitService
    @MockBean lateinit var sqsProducer: SqsProducer

    @AfterEach
    fun cleanup() {
        leads.deleteAll()
        orgs.deleteAll()
    }

    @Test
    fun `sem credencial do WhatsApp o lead e mantido`() {
        val org = orgs.save(Organization(name = "Loja sem WA", slug = "loja-sem-wa"))
        `when`(usageLimitService.canSendMessage(org.id, "WEBMOTORS")).thenReturn(true)

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
