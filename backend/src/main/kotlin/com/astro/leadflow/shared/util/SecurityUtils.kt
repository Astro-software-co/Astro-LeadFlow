package com.astro.leadflow.shared.util

import com.astro.leadflow.shared.exception.AccessDeniedException
import com.astro.leadflow.shared.modules.iam.domain.Role
import com.astro.leadflow.shared.modules.iam.domain.User
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

object SecurityUtils {

    fun getCurrentUser(): User {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        return principal as? User ?: throw AccessDeniedException("Usuário não autenticado.")
    }

    fun getCurrentOrganizationId(): UUID =
        getCurrentUser().organization?.id
            ?: throw AccessDeniedException("Usuário não vinculado a nenhuma organização.")

    fun assertOrganizationAccess(resourceOrganizationId: UUID) {
        val user = getCurrentUser()
        if (user.role == Role.SUPER_ADMIN) return
        if (user.organization?.id != resourceOrganizationId)
            throw AccessDeniedException("Acesso negado a recurso de outra organização.")
    }

    fun hasRole(role: Role): Boolean = getCurrentUser().role == role
    fun hasAnyRole(vararg roles: Role): Boolean = getCurrentUser().role in roles
}