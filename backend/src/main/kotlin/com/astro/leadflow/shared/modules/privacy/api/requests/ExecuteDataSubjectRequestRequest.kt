package com.astro.leadflow.shared.modules.privacy.api.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ExecuteDataSubjectRequestRequest(
    /** O mesmo identificador da criação; é conferido contra o hash guardado. */
    @field:NotBlank @field:Size(max = 200) val identifier: String?,
    /** Obrigatório para ACCESS e CORRECTION (atendidos manualmente pela controladora). */
    @field:Size(max = 2000) val resultSummary: String? = null
)
