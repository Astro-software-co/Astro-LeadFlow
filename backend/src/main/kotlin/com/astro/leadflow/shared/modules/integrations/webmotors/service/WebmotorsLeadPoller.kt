package com.astro.leadflow.shared.modules.integrations.webmotors.service

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Dispara o ciclo periódico. Só existe quando `integrations.webmotors.enabled=true`. */
@Component
@ConditionalOnProperty(prefix = "integrations.webmotors", name = ["enabled"], havingValue = "true")
class WebmotorsLeadPoller(private val coordinator: WebmotorsSyncCoordinator) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        initialDelayString = "\${integrations.webmotors.poll-initial-delay-ms:60000}",
        fixedDelayString = "\${integrations.webmotors.poll-interval-ms:300000}"
    )
    fun poll() {
        try {
            val ran = coordinator.syncAll()
            log.debug("Ciclo Webmotors concluído: {} conta(s) sincronizada(s).", ran)
        } catch (e: Exception) {
            log.error("Ciclo de polling Webmotors falhou: {}", e.message)
        }
    }
}
