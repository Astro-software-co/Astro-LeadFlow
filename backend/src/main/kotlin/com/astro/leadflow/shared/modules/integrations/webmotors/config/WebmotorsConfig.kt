package com.astro.leadflow.shared.modules.integrations.webmotors.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/** O agendamento é habilitado pelo SchedulingConfig global; o polling é ligado por `integrations.webmotors.enabled`. */
@Configuration
@EnableConfigurationProperties(WebmotorsProperties::class)
class WebmotorsConfig
