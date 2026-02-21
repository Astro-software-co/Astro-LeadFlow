package com.astro.leadflow.shared.modules.crm.service

import com.astro.leadflow.modules.crm.mapper.LeadMapper
import com.astro.leadflow.modules.iam.domain.Organization
import com.astro.leadflow.modules.iam.domain.User
import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.LeadStageHistoryRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.util.SecurityUtils
import io.mockk.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class PipelineServiceTest {

    private lateinit var leadRepository: LeadRepository
    private lateinit var historyRepository: LeadStageHistoryRepository
    private lateinit var leadService: LeadService
    private lateinit var leadMapper: LeadMapper
    private lateinit var pipelineService: PipelineService

    private val orgId = UUID.randomUUID()
    private val organization = Organization(id = orgId, name = "Test", slug = "test")

    @BeforeEach
    fun setup() {
        leadRepository = mockk()
        historyRepository = mockk()
        leadService = mockk()
        leadMapper = mockk()
        pipelineService = PipelineService(
            leadRepository, historyRepository, leadService, leadMapper
        )

        mockkObject(SecurityUtils)
        every {
            SecurityUtils.getCurrentOrganizationId()
        } returns orgId
        every {
            SecurityUtils.getCurrentUser()
        } returns mockk<User>(relaxed = true)
    }

    @Test
    fun `moveLead should update stage and save history`() {
        val lead = buildLead(PipelineStage.TALKING_TO_BOT)
        every { leadService.findLeadInOrg(lead.id, orgId) } returns lead
        every { leadRepository.save(lead) } returns lead
        every { historyRepository.save(any()) } returns mockk()
        every { leadMapper.toResponse(lead) } returns mockk()

        pipelineService.moveLead(lead.id, PipelineStage.WAITING_ATTENDANCE)

        Assertions.assertEquals(PipelineStage.WAITING_ATTENDANCE, lead.pipelineStage)
        verify(exactly = 1) { historyRepository.save(any()) }
    }

    @Test
    fun `moveLead should throw when target stage is same as current`() {
        val lead = buildLead(PipelineStage.IN_NEGOTIATION)
        every { leadService.findLeadInOrg(lead.id, orgId) } returns lead

        assertThrows<InvalidOperationException> {
            pipelineService.moveLead(lead.id, PipelineStage.IN_NEGOTIATION)
        }
    }

    @Test
    fun `moveLead should throw when lead is archived`() {
        val lead = buildLead(PipelineStage.ARCHIVED)
        every { leadService.findLeadInOrg(lead.id, orgId) } returns lead

        assertThrows<InvalidOperationException> {
            pipelineService.moveLead(lead.id, PipelineStage.WAITING_ATTENDANCE)
        }
    }

    @Test
    fun `advanceLead should move to next stage`() {
        val lead = buildLead(PipelineStage.TALKING_TO_BOT)
        every { leadService.findLeadInOrg(lead.id, orgId) } returns lead
        every { leadRepository.save(lead) } returns lead
        every { historyRepository.save(any()) } returns mockk()
        every { leadMapper.toResponse(lead) } returns mockk()

        pipelineService.advanceLead(lead.id)

        Assertions.assertEquals(PipelineStage.WAITING_ATTENDANCE, lead.pipelineStage)
    }

    @Test
    fun `advanceLead should throw when lead is at final stage`() {
        val lead = buildLead(PipelineStage.COMPLETED)
        every { leadService.findLeadInOrg(lead.id, orgId) } returns lead

        assertThrows<InvalidOperationException> {
            pipelineService.advanceLead(lead.id)
        }
    }

    private fun buildLead(stage: PipelineStage) = Lead(
        id = UUID.randomUUID(),
        organization = organization,
        phoneNumber = "+5511999999999",
        origin = "TEST",
        pipelineStage = stage
    )
}