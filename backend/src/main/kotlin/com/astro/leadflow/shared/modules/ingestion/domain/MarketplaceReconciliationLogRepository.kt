package com.astro.leadflow.shared.modules.ingestion.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MarketplaceReconciliationLogRepository : JpaRepository<MarketplaceReconciliationLog, UUID>
