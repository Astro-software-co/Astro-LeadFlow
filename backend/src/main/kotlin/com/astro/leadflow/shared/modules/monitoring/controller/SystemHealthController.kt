package com.astro.leadflow.shared.modules.monitoring.controller

import com.astro.leadflow.shared.modules.monitoring.controller.dto.SystemHealthResponse
import com.astro.leadflow.shared.modules.monitoring.service.SystemHealthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/system")
@PreAuthorize("hasRole('ASTRO_ADMIN')")
@Tag(name = "Admin — System", description = "Healthchecks operacionais mascarados")
class SystemHealthController(
    private val systemHealthService: SystemHealthService
) {

    @GetMapping("/health")
    @Operation(
        summary = "Healthcheck operacional mascarado",
        description = "Retorna flags booleanas de configuração e conectividade sem expor segredos."
    )
    fun health(): SystemHealthResponse = systemHealthService.current()
}
