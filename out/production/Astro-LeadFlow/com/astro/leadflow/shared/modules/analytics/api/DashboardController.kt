package com.astro.leadflow.modules.analytics.api

import com.astro.leadflow.modules.analytics.responses.*
import com.astro.leadflow.modules.analytics.service.DashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics — Dashboard", description = "KPIs e métricas do dashboard")
class DashboardController(
    private val dashboardService: DashboardService
) {

    @GetMapping("/kpis")
    @Operation(summary = "Retorna todos os KPIs do dashboard")
    fun getKpis(): KpiResponse =
        dashboardService.getKpis()

    @GetMapping("/charts/monthly-sales")
    @Operation(summary = "Evolução de vendas nos últimos 6 meses")
    fun getMonthlySales(): List<MonthlySalesPoint> =
        dashboardService.getMonthlySalesChart()

    @GetMapping("/charts/sales-by-category")
    @Operation(summary = "Vendas e estoque distribuídos por categoria de veículo")
    fun getSalesByCategory(): List<CategorySalesPoint> =
        dashboardService.getSalesByCategoryChart()
}