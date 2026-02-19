package com.astro.leadflow.shared.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

open class BusinessException(message: String) : RuntimeException(message)
class ResourceNotFoundException(resource: String, id: Any) : BusinessException("$resource não encontrado(a): $id")
class AccessDeniedException(message: String = "Acesso negado.") : BusinessException(message)
class QuotaExceededException(message: String = "Limite de mensagens atingido.") : BusinessException(message)

data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val details: List<String> = emptyList()
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<ErrorResponse> {
        val status = when (ex) {
            is ResourceNotFoundException -> HttpStatus.NOT_FOUND
            is AccessDeniedException -> HttpStatus.FORBIDDEN
            is QuotaExceededException -> HttpStatus.PAYMENT_REQUIRED
            else -> HttpStatus.BAD_REQUEST
        }
        return ResponseEntity.status(status).body(
            ErrorResponse(status = status.value(), error = status.reasonPhrase, message = ex.message ?: "Erro.")
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(
            ErrorResponse(status = 400, error = "Validation Error", message = "Campos inválidos.", details = details)
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity.internalServerError().body(
            ErrorResponse(status = 500, error = "Internal Server Error", message = "Erro interno.")
        )
}