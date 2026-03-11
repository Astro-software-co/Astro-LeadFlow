package com.astro.leadflow.shared.modules.analytics.service

import com.astro.leadflow.shared.modules.analytics.api.responses.CategorySalesPoint
import com.astro.leadflow.shared.modules.analytics.api.responses.KpiCard
import com.astro.leadflow.shared.modules.analytics.api.responses.KpiResponse
import com.astro.leadflow.shared.modules.analytics.api.responses.KpiUnit
import com.astro.leadflow.shared.modules.analytics.api.responses.MonthlySalesPoint
import com.astro.leadflow.shared.modules.communication.domain.ChatMessageRepository
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.modules.inventory.domain.VehicleRepository
import com.astro.leadflow.shared.modules.inventory.domain.VehicleStatus
import com.astro.leadflow.shared.util.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class DashboardService(
    private val leadRepository: LeadRepository,
    private val vehicleRepository: VehicleRepository,
    private val chatMessageRepository: ChatMessageRepository
) {

    @Transactional(readOnly = true)
    fun getKpis(): KpiResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val now = LocalDate.now()
        val lastMonth = now.minusMonths(1)

        val totalLeads = leadRepository.countByOrganizationId(orgId)
        val totalLeadsLastMonth = leadRepository.countByOrganizationIdAndPeriod(
            orgId, lastMonth.monthValue, lastMonth.year
        )
        val totalLeadsThisMonth = leadRepository.countByOrganizationIdAndPeriod(
            orgId, now.monthValue, now.year
        )

        val completedLeads = leadRepository.countByOrganizationIdAndStage(
            orgId, PipelineStage.COMPLETED
        )
        val conversionRate = if (totalLeads > 0)
            (completedLeads.toDouble() / totalLeads.toDouble()) * 100 else 0.0

        val completedLastMonth = leadRepository.countByOrganizationIdAndStageAndPeriod(
            orgId, PipelineStage.COMPLETED, lastMonth.monthValue, lastMonth.year
        )
        val totalLastMonthForRate = if (totalLeadsLastMonth > 0) totalLeadsLastMonth else 1
        val conversionRateLastMonth =
            (completedLastMonth.toDouble() / totalLastMonthForRate.toDouble()) * 100

        val stockCounts = vehicleRepository.countByStatus(orgId)
            .associate { VehicleStatus.valueOf(it[0].toString()) to it[1].toString().toLong() }

        val totalStock = stockCounts.values.sum()
        val availableStock = stockCounts[VehicleStatus.AVAILABLE] ?: 0L
        val soldThisMonth = leadRepository.countSalesByOrganizationAndPeriod(
            orgId, now.monthValue, now.year
        )
        val soldLastMonth = leadRepository.countSalesByOrganizationAndPeriod(
            orgId, lastMonth.monthValue, lastMonth.year
        )

        val revenueThisMonth = leadRepository.sumRevenueByOrganizationAndPeriod(
            orgId, now.monthValue, now.year
        ) ?: 0.0
        val revenueLastMonth = leadRepository.sumRevenueByOrganizationAndPeriod(
            orgId, lastMonth.monthValue, lastMonth.year
        ) ?: 0.0

        val averageTicket = if (soldThisMonth > 0) revenueThisMonth / soldThisMonth else 0.0
        val averageTicketLastMonth = if (soldLastMonth > 0) revenueLastMonth / soldLastMonth else 0.0

        val avgResponseHours = chatMessageRepository
            .avgResponseTimeHoursByOrganization(orgId) ?: 0.0

        return KpiResponse(
            salesThisMonth = KpiCard(soldThisMonth.toDouble(), soldLastMonth.toDouble(), KpiUnit.NUMBER),
            totalRevenue = KpiCard(revenueThisMonth, revenueLastMonth, KpiUnit.CURRENCY),
            vehiclesInStock = KpiCard(totalStock.toDouble(), availableStock.toDouble(), KpiUnit.NUMBER),
            averageTicket = KpiCard(averageTicket, averageTicketLastMonth, KpiUnit.CURRENCY),
            leadsConverted = KpiCard(conversionRate, conversionRateLastMonth, KpiUnit.PERCENTAGE),
            avgResponseTime = KpiCard(avgResponseHours, 0.0, KpiUnit.HOURS),
            totalLeads = KpiCard(totalLeadsThisMonth.toDouble(), totalLeadsLastMonth.toDouble(), KpiUnit.NUMBER)
        )
    }

    @Transactional(readOnly = true)
    fun getMonthlySalesChart(): List<MonthlySalesPoint> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val now = LocalDate.now()

        return (5 downTo 0).map { monthsAgo ->
            val date = now.minusMonths(monthsAgo.toLong())
            val sales = leadRepository.countSalesByOrganizationAndPeriod(
                orgId, date.monthValue, date.year
            )
            val revenue = leadRepository.sumRevenueByOrganizationAndPeriod(
                orgId, date.monthValue, date.year
            ) ?: 0.0

            MonthlySalesPoint(
                month = date.month.name.take(3)
                    .lowercase()
                    .replaceFirstChar { it.uppercase() },
                sales = sales,
                revenue = revenue
            )
        }
    }

    @Transactional(readOnly = true)
    fun getSalesByCategoryChart(): List<CategorySalesPoint> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return vehicleRepository.countSoldAndStockByCategory(orgId)
            .map {
                CategorySalesPoint(
                    category = it[0].toString(),
                    sold = it[1].toString().toLong(),
                    inStock = it[2].toString().toLong()
                )
            }
    }
}