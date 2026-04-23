package com.astro.leadflow.shared.modules.saas.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlanRepository : JpaRepository<Plan, UUID>
