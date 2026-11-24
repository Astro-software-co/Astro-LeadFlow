package com.astro.leadflow.shared.modules.privacy.api.requests

import com.astro.leadflow.shared.modules.privacy.domain.DataSubjectRequestType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateDataSubjectRequestRequest(
    @field:NotNull val type: DataSubjectRequestType?,
    /** Telefone ou e-mail do titular. Não é gravado: só o HMAC e uma dica mascarada. */
    @field:NotBlank @field:Size(max = 200) val identifier: String?,
    @field:Size(max = 30) val channel: String? = null,
    /** Quando a controladora recebeu o pedido; padrão: agora. */
    val receivedAt: LocalDateTime? = null
)
