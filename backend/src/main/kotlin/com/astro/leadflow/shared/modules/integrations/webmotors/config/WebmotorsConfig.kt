package com.astro.leadflow.shared.modules.integrations.webmotors.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableConfigurationProperties(WebmotorsProperties::class)
class WebmotorsConfig

/** Só habilita o agendamento quando a integração está ligada. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "integrations.webmotors", name = ["enabled"], havingValue = "true")
class WebmotorsSchedulingConfig
