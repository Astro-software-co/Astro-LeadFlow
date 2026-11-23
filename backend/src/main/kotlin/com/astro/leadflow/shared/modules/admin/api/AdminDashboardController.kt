package com.astro.leadflow.shared.modules.admin.api

import com.astro.leadflow.shared.modules.admin.api.responses.AdminDashboardResponse
import com.astro.leadflow.shared.modules.admin.service.AdminDashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('ASTRO_ADMIN')")
@Tag(name = "Admin - Finance Dashboard", description = "Financial SaaS metrics for Astro admins")
class AdminDashboardController(
    private val adminDashboardService: AdminDashboardService
) {

    @GetMapping
    @Operation(summary = "Returns admin finance dashboard metrics and chart series")
    fun getDashboard(): AdminDashboardResponse = adminDashboardService.getDashboard()
}
