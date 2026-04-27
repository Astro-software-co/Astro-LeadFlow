package com.astro.leadflow.shared.modules.iam.api

import com.astro.leadflow.shared.modules.iam.api.requests.ChangePasswordRequest
import com.astro.leadflow.shared.modules.iam.api.requests.CreateUserRequest
import com.astro.leadflow.shared.modules.iam.api.requests.UpdateNotificationsRequest
import com.astro.leadflow.shared.modules.iam.api.requests.UpdateTeamUserRequest
import com.astro.leadflow.shared.modules.iam.api.requests.UpdateTwoFactorRequest
import com.astro.leadflow.shared.modules.iam.api.requests.UpdateUserRequest
import com.astro.leadflow.shared.modules.iam.api.requests.UpdateUserRoleRequest
import com.astro.leadflow.shared.modules.iam.api.responses.AssignableUserResponse
import com.astro.leadflow.shared.modules.iam.api.responses.UserResponse
import com.astro.leadflow.shared.modules.iam.api.responses.UserSummaryResponse
import com.astro.leadflow.shared.modules.iam.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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

    @GetMapping("/me")
    @Operation(summary = "Retorna dados do usuário logado")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Dados retornados com sucesso"),
        ApiResponse(responseCode = "401", description = "Não autenticado",
            content = [Content(schema = Schema(ref = "#/components/schemas/ApiError"))])
    )
    fun getMe(): UserResponse = userService.getMe()

    @PatchMapping("/me")
    @Operation(summary = "Atualiza nome, telefone ou avatar do usuário logado")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Perfil atualizado"),
        ApiResponse(responseCode = "400", description = "Dados inválidos")
    )
    fun updateMe(@Valid @RequestBody request: UpdateUserRequest): UserResponse =
        userService.updateMe(request)

    @PatchMapping("/me/password")
    @Operation(summary = "Altera senha do usuário logado")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
        ApiResponse(responseCode = "400", description = "Senha atual incorreta ou senhas não coincidem")
    )
    fun changePassword(@Valid @RequestBody request: ChangePasswordRequest): UserResponse =
        userService.changePassword(request)

    @PatchMapping("/me/notifications")
    @Operation(summary = "Atualiza preferências de notificação")
    @ApiResponse(responseCode = "200", description = "Preferências atualizadas com sucesso")
    fun updateNotifications(
        @Valid @RequestBody request: UpdateNotificationsRequest
    ): UserResponse = userService.updateNotifications(request)

    @PatchMapping("/me/two-factor")
    @Operation(summary = "Ativa ou desativa autenticação em dois fatores")
    @ApiResponse(responseCode = "200", description = "2FA atualizado com sucesso")
    fun updateTwoFactor(
        @Valid @RequestBody request: UpdateTwoFactorRequest
    ): UserResponse = userService.updateTwoFactor(request)

    @GetMapping
    @Operation(summary = "Lista usuários da organização")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @PreAuthorize("hasRole('MANAGER')")
    fun listUsers(): List<UserSummaryResponse> =
        userService.listOrganizationUsers()

    @GetMapping("/assignable")
    @Operation(summary = "Lista usuarios que podem receber leads")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    fun listAssignable(): List<AssignableUserResponse> =
        userService.listAssignableUsers()

    @GetMapping("/{id}")
    @Operation(summary = "Busca usuário da organização por ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Usuário encontrado"),
        ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    )
    @PreAuthorize("hasRole('MANAGER')")
    fun findById(
        @Parameter(description = "ID do usuário") @PathVariable id: UUID
    ): UserResponse = userService.findById(id)

    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Convida novo usuário para a organização",
        description = "Cria a conta com senha temporária e status PENDING_VERIFICATION"
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
        ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    )
    @PreAuthorize("hasRole('MANAGER')")
    fun invite(@Valid @RequestBody request: CreateUserRequest): UserResponse =
        userService.inviteUser(request)

    @PatchMapping("/{id}")
    @Operation(summary = "Atualiza dados de um seller da organização")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
        ApiResponse(responseCode = "403", description = "Sem permissão para esta operação")
    )
    @PreAuthorize("hasRole('MANAGER')")
    fun updateTeamUser(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateTeamUserRequest
    ): UserResponse = userService.updateTeamUser(id, request)

    @PatchMapping("/{id}/role")
    @Operation(summary = "Altera role de um usuário")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Role alterada com sucesso"),
        ApiResponse(responseCode = "403", description = "Sem permissão para esta operação")
    )
    @PreAuthorize("hasRole('MANAGER')")
    fun updateRole(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUserRoleRequest
    ): UserResponse = userService.updateUserRole(id, request)

    @PatchMapping("/{id}/suspend")
    @Operation(summary = "Suspende acesso de um usuário")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Usuário suspenso com sucesso"),
        ApiResponse(responseCode = "422", description = "Usuário já suspenso")
    )
    @PreAuthorize("hasRole('MANAGER')")
    fun suspend(@PathVariable id: UUID): UserResponse =
        userService.suspendUser(id)

    @PatchMapping("/{id}/reactivate")
    @Operation(summary = "Reativa usuário suspenso")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Usuário reativado com sucesso"),
        ApiResponse(responseCode = "422", description = "Usuário já está ativo")
    )
    @PreAuthorize("hasRole('MANAGER')")
    fun reactivate(@PathVariable id: UUID): UserResponse =
        userService.reactivateUser(id)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove usuário da organização")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Usuário removido com sucesso"),
        ApiResponse(responseCode = "422", description = "Não é possível remover a própria conta")
    )
    @PreAuthorize("hasRole('MANAGER')")
    fun delete(@PathVariable id: UUID) =
        userService.deleteUser(id)
}
