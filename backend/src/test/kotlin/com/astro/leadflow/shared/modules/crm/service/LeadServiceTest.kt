package com.astro.leadflow.shared.modules.crm.service

import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.crm.api.requests.LeadCreateRequest
import com.astro.leadflow.shared.modules.crm.api.requests.LeadUpdateRequest
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.modules.crm.mapper.LeadMapper
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.inventory.domain.VehicleRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class LeadServiceTest {

    private lateinit var leadRepository: LeadRepository
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var leadMapper: LeadMapper
    private lateinit var leadService: LeadService

    private val orgId = UUID.randomUUID()
    private val organization = Organization(id = orgId, name = "Test Motors", slug = "test")

    // mockkObject é global na JVM: sem desfazer, o mock vaza para os testes seguintes.
    @AfterEach
    fun unmockSecurityUtils() = unmockkObject(com.astro.leadflow.shared.util.SecurityUtils)

    @BeforeEach
    fun setup() {
        leadRepository = mockk()
        organizationRepository = mockk()
        vehicleRepository = mockk()
        leadMapper = mockk()
        leadService = LeadService(
            leadRepository,
            organizationRepository,
            vehicleRepository,
            leadMapper
        )

        mockkObject(com.astro.leadflow.shared.util.SecurityUtils)
        every {
            com.astro.leadflow.shared.util.SecurityUtils.getCurrentOrganizationId()
        } returns orgId
    }

    @Test
    fun `create should save lead and return response`() {
        val request = LeadCreateRequest(
            phoneNumber = "+5511999999999",
            origin = "WEBMOTORS"
        )
        val lead = buildLead()

        every { organizationRepository.findById(orgId) } returns Optional.of(organization)
        every {
            leadRepository.existsByOrganizationIdAndPhoneNumber(orgId, request.phoneNumber)
        } returns false
        every { leadRepository.save(any()) } returns lead
        every { leadMapper.toResponse(lead) } returns mockk()

        val result = leadService.create(request)

        Assertions.assertNotNull(result)
        verify(exactly = 1) { leadRepository.save(any()) }
    }

    @Test
    fun `create should mark as duplicate when phone already exists`() {
        val request = LeadCreateRequest(
            phoneNumber = "+5511999999999",
            origin = "OLX"
        )
        val lead = buildLead(isDuplicate = true)

        every { organizationRepository.findById(orgId) } returns Optional.of(organization)
        every {
            leadRepository.existsByOrganizationIdAndPhoneNumber(orgId, request.phoneNumber)
        } returns true
        every { leadRepository.save(any()) } returns lead
        every { leadMapper.toResponse(lead) } returns mockk()

        leadService.create(request)

        verify {
            leadRepository.save(match { it.isDuplicate })
        }
    }

    @Test
    fun `findById should throw when lead not found`() {
        val leadId = UUID.randomUUID()
        every {
            leadRepository.findByOrganizationIdAndId(orgId, leadId)
        } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            leadService.findById(leadId)
        }
    }

    @Test
    fun `archive should throw when lead is already archived`() {
        val lead = buildLead(stage = PipelineStage.ARCHIVED)
        every {
            leadRepository.findByOrganizationIdAndId(orgId, lead.id)
        } returns Optional.of(lead)

        assertThrows<InvalidOperationException> {
            leadService.archive(lead.id)
        }
    }

    @Test
    fun `update should apply only non-null fields`() {
        val lead = buildLead()
        val request = LeadUpdateRequest(
            name = "João Silva",
            notes = "Cliente VIP"
        )

        every {
            leadRepository.findByOrganizationIdAndId(orgId, lead.id)
        } returns Optional.of(lead)
        every { leadRepository.save(lead) } returns lead
        every { leadMapper.toResponse(lead) } returns mockk()

        leadService.update(lead.id, request)

        Assertions.assertEquals("João Silva", lead.name)
        Assertions.assertEquals("Cliente VIP", lead.notes)
        verify(exactly = 1) { leadRepository.save(lead) }
    }

    @Test
    fun `findAll should delegate filters with inclusive date range bounds`() {
        val lead = buildLead()
        val pageable = PageRequest.of(0, 20)
        val assignedTo = UUID.randomUUID()
        val dateFrom = LocalDate.of(2026, 4, 1)
        val dateTo = LocalDate.of(2026, 4, 5)

        every {
            leadRepository.findPageByOrganization(any(), any(), any(), any(), any(), any(), any(), any())
        } returns PageImpl(listOf(lead), pageable, 1)
        every { leadMapper.toResponse(lead) } returns mockk(relaxed = true)

        leadService.findAll(
            search = "  joao  ",
            origin = "  WebMotors  ",
            stage = PipelineStage.WAITING_ATTENDANCE,
            assignedTo = assignedTo,
            dateFrom = dateFrom,
            dateTo = dateTo,
            pageable = pageable
        )

        verify(exactly = 1) {
            leadRepository.findPageByOrganization(
                orgId = orgId,
                search = "joao",
                origin = "WebMotors",
                stage = PipelineStage.WAITING_ATTENDANCE,
                assignedTo = assignedTo,
                createdFrom = dateFrom.atStartOfDay(),
                createdTo = dateTo.plusDays(1).atStartOfDay(),
                pageable = pageable
            )
        }
    }

    private fun buildLead(
        stage: PipelineStage = PipelineStage.TALKING_TO_BOT,
        isDuplicate: Boolean = false
    ) = Lead(
        id = UUID.randomUUID(),
        organization = organization,
        phoneNumber = "+5511999999999",
        origin = "WEBMOTORS",
        pipelineStage = stage,
        isDuplicate = isDuplicate
    )
}
