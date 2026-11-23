package com.astro.leadflow.shared.modules.monitoring.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.monitoring")
data class MonitoringConfig(
    val errorRetentionDays: Long = 90,
    val realtimePollingSeconds: Long = 10
)
