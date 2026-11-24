package com.astro.leadflow.shared.modules.privacy.api.responses

import com.astro.leadflow.shared.modules.privacy.domain.DataSubjectRequestStatus
import com.astro.leadflow.shared.modules.privacy.domain.DataSubjectRequestType
import java.time.LocalDateTime
import java.util.UUID

data class DataSubjectRequestResponse(
    val id: UUID,
    val type: DataSubjectRequestType,
    val status: DataSubjectRequestStatus,
    val subjectHint: String?,
    val channel: String?,
    val receivedAt: LocalDateTime,
    val dueAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val overdue: Boolean,
    val resultSummary: String?
)
