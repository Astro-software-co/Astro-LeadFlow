package com.astro.leadflow.shared.modules.analytics.api

import com.astro.leadflow.shared.modules.analytics.api.responses.CategorySalesPoint
import com.astro.leadflow.shared.modules.analytics.api.responses.KpiResponse
import com.astro.leadflow.shared.modules.analytics.api.responses.MonthlySalesPoint
import com.astro.leadflow.shared.modules.analytics.service.DashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
    @ApiResponse(responseCode = "200", description = "KPIs retornados com sucesso")
    fun getKpis(): KpiResponse =
        dashboardService.getKpis()

    @GetMapping("/charts/monthly-sales")
    @Operation(summary = "Evolução de vendas nos últimos 6 meses")
    @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso")
    fun getMonthlySales(): List<MonthlySalesPoint> =
        dashboardService.getMonthlySalesChart()

    @GetMapping("/charts/sales-by-category")
    @Operation(summary = "Vendas e estoque distribuídos por categoria de veículo")
    @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso")
    fun getSalesByCategory(): List<CategorySalesPoint> =
        dashboardService.getSalesByCategoryChart()
}