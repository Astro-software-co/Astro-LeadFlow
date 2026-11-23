package com.astro.leadflow.shared.modules.integrations.webmotors.api.requests

import com.astro.leadflow.shared.modules.integrations.webmotors.domain.AcceptMode
import jakarta.validation.constraints.Size

data class WebmotorsAccountRequest(
    @field:Size(max = 200) val clientId: String? = null,
    @field:Size(max = 200) val clientSecret: String? = null,
    @field:Size(max = 200) val username: String? = null,
    @field:Size(max = 200) val password: String? = null,
    @field:Size(max = 100) val storeId: String? = null,
    val acceptMode: AcceptMode? = null,
    val welcomeMessageEnabled: Boolean? = null,
    val enabled: Boolean? = null
)
