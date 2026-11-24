package com.astro.leadflow.shared.exception

import org.springframework.http.HttpStatus

/** Destinatário que não pode receber mensagens do sistema (lead anonimizado; depois, consentimento negado/revogado). */
class RecipientBlockedException(
    message: String = "Este contato não pode receber mensagens do sistema."
) : BusinessException(message, HttpStatus.UNPROCESSABLE_ENTITY)
