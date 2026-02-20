package com.astro.leadflow.modules.iam.api

import com.astro.leadflow.modules.iam.api.requests.*
import com.astro.leadflow.modules.iam.api.responses.UserResponse
import com.astro.leadflow.modules.iam.api.responses.UserSummaryResponse
import com.astro.leadflow.modules.iam.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "IAM — Usuários", description = "Gestão de usuários e preferências")
class UserController(private val userService: UserService) {

    // ── Perfil do usuário logado ──────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Retorna dados do usuário logado")
    fun getMe(): UserResponse = userService.getMe()

    @PatchMapping("/me")
    @Operation(summary = "Atualiza perfil do usuário logado")
    fun updateMe(@Valid @RequestBody request: UpdateUserRequest): UserResponse =
        userService.updateMe(request)

    @PatchMapping("/me/password")
    @Operation(summary = "Altera senha do usuário logado")
    fun changePassword(@Valid @RequestBody request: ChangePasswordRequest): UserResponse =
        userService.changePassword(request)

    @PatchMapping("/me/notifications")
    @Operation(summary = "Atualiza preferências de notificação")
    fun updateNotifications(
        @Valid @RequestBody request: UpdateNotificationsRequest
    ): UserResponse = userService.updateNotifications(request)

    @PatchMapping("/me/two-factor")
    @Operation(summary = "Ativa ou desativa 2FA por e-mail e SMS")
    fun updateTwoFactor(
        @Valid @RequestBody request: UpdateTwoFactorRequest
    ): UserResponse = userService.updateTwoFactor(request)

    // ── Gestão de usuários da organização (Admin/Manager) ────

    @GetMapping
    @Operation(summary = "Lista usuários da organização")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    fun listUsers(): List<UserSummaryResponse> =
        userService.listOrganizationUsers()

    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Convida novo usuário para a organização")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    fun invite(@Valid @RequestBody request: CreateUserRequest): UserResponse =
        userService.inviteUser(request)

    @PatchMapping("/{id}/role")
    @Operation(summary = "Altera role de um usuário da organização")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateRole(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUserRoleRequest
    ): UserResponse = userService.updateUserRole(id, request)

    @PatchMapping("/{id}/suspend")
    @Operation(summary = "Suspende usuário da organização")
    @PreAuthorize("hasRole('ADMIN')")
    fun suspend(@PathVariable id: UUID): UserResponse =
        userService.suspendUser(id)

    @PatchMapping("/{id}/reactivate")
    @Operation(summary = "Reativa usuário suspenso")
    @PreAuthorize("hasRole('ADMIN')")
    fun reactivate(@PathVariable id: UUID): UserResponse =
        userService.reactivateUser(id)
}