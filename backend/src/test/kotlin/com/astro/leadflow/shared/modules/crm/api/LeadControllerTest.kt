package com.astro.leadflow.shared.modules.crm.api

import com.astro.leadflow.shared.modules.crm.api.responses.LeadResponse
import com.astro.leadflow.shared.modules.crm.api.responses.StageHistoryResponse
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.modules.crm.service.LeadAssignmentService
import com.astro.leadflow.shared.modules.crm.service.LeadService
import com.astro.leadflow.shared.modules.crm.service.PipelineService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class LeadControllerTest {

    private val leadService = mockk<LeadService>()
    private val pipelineService = mockk<PipelineService>()
    private val assignmentService = mockk<LeadAssignmentService>()

    private val controller = LeadController(
        leadService = leadService,
        pipelineService = pipelineService,
        assignmentService = assignmentService
    )

    @Test
    fun `findAll should apply pagination guard rails and delegate advanced filters`() {
        val assignedTo = UUID.randomUUID()
        val expected: Page<LeadResponse> = PageImpl(emptyList())
        val pageableSlot = slot<Pageable>()

        every {
            leadService.findAll(
                search = "joao",
                origin = "WEBMOTORS",
                stage = PipelineStage.WAITING_ATTENDANCE,
                assignedTo = assignedTo,
                dateFrom = LocalDate.of(2026, 4, 1),
                dateTo = LocalDate.of(2026, 4, 5),
                pageable = capture(pageableSlot)
            )
        } returns expected

        val result = controller.findAll(
            search = "joao",
            origin = "WEBMOTORS",
            stage = PipelineStage.WAITING_ATTENDANCE,
            assignedTo = assignedTo,
            dateFrom = LocalDate.of(2026, 4, 1),
            dateTo = LocalDate.of(2026, 4, 5),
            page = -5,
            size = 999
        )

        assertSame(expected, result)
        assertEquals(0, pageableSlot.captured.pageNumber)
        assertEquals(100, pageableSlot.captured.pageSize)
        assertEquals("updatedAt: DESC", pageableSlot.captured.sort.toString())
    }

    @Test
    fun `findActivities should delegate to pipeline service`() {
        val leadId = UUID.randomUUID()
        val expected = listOf(
            StageHistoryResponse(
                fromStage = PipelineStage.TALKING_TO_BOT,
                toStage = PipelineStage.WAITING_ATTENDANCE,
                movedBy = null,
                movedAt = LocalDateTime.of(2026, 4, 5, 14, 0)
            )
        )

        every { pipelineService.getLeadActivities(leadId) } returns expected

        val result = controller.findActivities(leadId)

        assertEquals(expected, result)
        verify(exactly = 1) { pipelineService.getLeadActivities(leadId) }
    }
}
