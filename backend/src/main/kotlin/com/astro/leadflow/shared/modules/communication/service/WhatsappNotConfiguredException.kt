package com.astro.leadflow.shared.modules.communication.service

/** Organização sem credencial do WhatsApp no modo real. Continua sendo um `IllegalArgumentException` para quem já trata assim. */
class WhatsappNotConfiguredException(message: String) : IllegalArgumentException(message)
