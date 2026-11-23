package com.astro.leadflow.shared.modules.integrations.webmotors.service

/** Lead pronto para entrar no CRM (contato já validado). */
data class IncomingLead(
    val name: String?,
    /** Telefone normalizado em E.164 (+55...). */
    val phone: String,
    val email: String?,
    val origin: String,
    val vehicleInterest: String?,
    val notes: String?,
    val sendWelcomeMessage: Boolean
)
