package com.astro.leadflow.shared.exception

import org.springframework.http.HttpStatus

open class BusinessException(
    message: String,
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message)

class ResourceNotFoundException(resource: String, id: Any) : BusinessException(
    message = "$resource não encontrado(a) com identificador: $id",
    httpStatus = HttpStatus.NOT_FOUND
)

class AccessDeniedException(
    message: String = "Acesso negado."
) : BusinessException(message, HttpStatus.FORBIDDEN)

class QuotaExceededException(
    message: String = "Limite de mensagens atingido para este período."
) : BusinessException(message, HttpStatus.PAYMENT_REQUIRED)

class DuplicateResourceException(resource: String, field: String, value: Any) : BusinessException(
    message = "$resource com $field '$value' já existe.",
    httpStatus = HttpStatus.CONFLICT
)

class InvalidOperationException(message: String) : BusinessException(
    message = message,
    httpStatus = HttpStatus.UNPROCESSABLE_ENTITY
)

class ExternalServiceException(service: String, message: String) : BusinessException(
    message = "Erro ao comunicar com serviço externo '$service': $message",
    httpStatus = HttpStatus.BAD_GATEWAY
)