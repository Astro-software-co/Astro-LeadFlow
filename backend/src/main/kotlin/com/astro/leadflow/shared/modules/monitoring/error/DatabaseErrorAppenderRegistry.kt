package com.astro.leadflow.shared.modules.monitoring.error

object DatabaseErrorAppenderRegistry {
    @Volatile
    var writer: ApplicationErrorWriter? = null
}
