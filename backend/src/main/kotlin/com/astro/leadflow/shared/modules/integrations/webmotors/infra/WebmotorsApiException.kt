package com.astro.leadflow.shared.modules.integrations.webmotors.infra

class WebmotorsApiException(
    message: String,
    val httpStatus: Int? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)
