package com.astro.leadflow.shared.exception

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException as SpringAccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(this::class.java)

    // ── Exceções de negócio ────────────────────────────────────────

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        log.warn("BusinessException [${ex.httpStatus}]: ${ex.message}")
        return ResponseEntity.status(ex.httpStatus).body(
            ApiError(
                status = ex.httpStatus.value(),
                error = ex.httpStatus.reasonPhrase,
                message = ex.message ?: "Erro de negócio.",
                path = request.requestURI
            )
        )
    }

    // ── Validação de campos (@Valid) ───────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val fieldErrors = ex.bindingResult.fieldErrors.map {
            FieldError(field = it.field, message = it.defaultMessage ?: "Inválido")
        }
        log.warn("Validation failed: ${fieldErrors.size} field(s) inválido(s)")
        return ResponseEntity.badRequest().body(
            ApiError(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Validation Error",
                message = "Um ou mais campos possuem valores inválidos.",
                path = request.requestURI,
                details = fieldErrors
            )
        )
    }

    // ── Corpo da requisição ilegível ───────────────────────────────

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        log.warn("Request body ilegível: ${ex.message}")
        return ResponseEntity.badRequest().body(
            ApiError(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Malformed Request",
                message = "Corpo da requisição inválido ou mal formatado.",
                path = request.requestURI
            )
        )
    }

    // ── Parâmetro com tipo errado ──────────────────────────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val message = "Parâmetro '${ex.name}' com valor '${ex.value}' é inválido."
        return ResponseEntity.badRequest().body(
            ApiError(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Type Mismatch",
                message = message,
                path = request.requestURI
            )
        )
    }

    // ── Parâmetro obrigatório ausente ──────────────────────────────

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.badRequest().body(
            ApiError(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Missing Parameter",
                message = "Parâmetro obrigatório ausente: '${ex.parameterName}'.",
                path = request.requestURI
            )
        )
    }

    // ── Método HTTP não suportado ──────────────────────────────────

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
            ApiError(
                status = HttpStatus.METHOD_NOT_ALLOWED.value(),
                error = "Method Not Allowed",
                message = "Método '${ex.method}' não permitido para este endpoint.",
                path = request.requestURI
            )
        )
    }

    // ── Segurança Spring ───────────────────────────────────────────

    @ExceptionHandler(SpringAccessDeniedException::class)
    fun handleSpringAccessDenied(
        ex: SpringAccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiError(
                status = HttpStatus.FORBIDDEN.value(),
                error = "Forbidden",
                message = "Você não tem permissão para acessar este recurso.",
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(
        ex: BadCredentialsException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiError(
                status = HttpStatus.UNAUTHORIZED.value(),
                error = "Unauthorized",
                message = "Credenciais inválidas.",
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(DisabledException::class)
    fun handleDisabled(
        ex: DisabledException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiError(
                status = HttpStatus.UNAUTHORIZED.value(),
                error = "Account Disabled",
                message = "Conta inativa ou suspensa.",
                path = request.requestURI
            )
        )
    }

    // ── Fallback geral ─────────────────────────────────────────────

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        log.error("Erro inesperado em ${request.requestURI}: ${ex.message}", ex)
        return ResponseEntity.internalServerError().body(
            ApiError(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                error = "Internal Server Error",
                message = "Ocorreu um erro inesperado. Tente novamente ou contate o suporte.",
                path = request.requestURI
            )
        )
    }
}