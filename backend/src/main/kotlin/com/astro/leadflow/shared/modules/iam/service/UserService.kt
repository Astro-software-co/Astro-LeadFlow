package com.astro.leadflow.shared.modules.iam.service

import com.astro.leadflow.shared.modules.iam.api.requests.ChangePasswordRequest
import com.astro.leadflow.shared.modules.iam.api.requests.CreateUserRequest
import com.astro.leadflow.shared.modules.iam.api.requests.UpdateNotificationsRequest
import com.astro.leadflow.shared.modules.iam.api.requests.UpdateTwoFactorRequest
import com.astro.leadflow.shared.modules.iam.api.requests.UpdateUserRequest
import com.astro.leadflow.shared.modules.iam.api.requests.UpdateUserRoleRequest
import com.astro.leadflow.shared.modules.iam.api.responses.UserResponse
import com.astro.leadflow.shared.modules.crm.api.responses.UserSummaryResponse
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.iam.domain.Role
import com.astro.leadflow.shared.modules.iam.domain.User
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.modules.iam.domain.UserStatus
import com.astro.leadflow.shared.modules.iam.mapper.UserMapper
import com.astro.leadflow.shared.exception.AccessDeniedException
import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.DuplicateResourceException
import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.util.SecurityUtils
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userMapper: UserMapper
) {

    @Transactional(readOnly = true)
    fun getMe(): UserResponse {
        val userId = SecurityUtils.getCurrentUser().id
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User", userId) }
        return userMapper.toResponse(user)
    }

    @Transactional
    fun updateMe(request: UpdateUserRequest): UserResponse {
        val user = SecurityUtils.getCurrentUser()
        request.name?.let { value -> user.name = value }
        request.phoneNumber?.let { value -> user.phoneNumber = value }
        request.avatarUrl?.let { value -> user.avatarUrl = value }
        return userMapper.toResponse(userRepository.save(user))
    }

    @Transactional
    fun changePassword(request: ChangePasswordRequest): UserResponse {
        val user = SecurityUtils.getCurrentUser()
        if (!passwordEncoder.matches(request.currentPassword, user.password))
            throw BusinessException("Senha atual incorreta.")
        if (request.newPassword != request.confirmNewPassword)
            throw InvalidOperationException("A nova senha e a confirmação não coincidem.")
        user.updatePassword(passwordEncoder.encode(request.newPassword))
        return userMapper.toResponse(userRepository.save(user))
    }

    @Transactional
    fun updateNotifications(request: UpdateNotificationsRequest): UserResponse {
        val user = SecurityUtils.getCurrentUser()
        request.notifyNewLead?.let { value -> user.notifyNewLead = value }
        request.notifyLeadWaiting?.let { value -> user.notifyLeadWaiting = value }
        request.notifySaleCompleted?.let { value -> user.notifySaleCompleted = value }
        request.notifyCardMoved?.let { value -> user.notifyCardMoved = value }
        request.notifyViaEmail?.let { value -> user.notifyViaEmail = value }
        request.notifyViaPush?.let { value -> user.notifyViaPush = value }
        return userMapper.toResponse(userRepository.save(user))
    }

    @Transactional
    fun updateTwoFactor(request: UpdateTwoFactorRequest): UserResponse {
        val user = SecurityUtils.getCurrentUser()
        request.emailEnabled?.let { value -> user.twoFactorEmailEnabled = value }
        request.smsEnabled?.let { value -> user.twoFactorSmsEnabled = value }
        return userMapper.toResponse(userRepository.save(user))
    }

    @Transactional
    fun inviteUser(request: CreateUserRequest): UserResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val organization = organizationRepository.findById(orgId)
            .orElseThrow { ResourceNotFoundException("Organization", orgId) }

        if (userRepository.existsByEmail(request.email))
            throw DuplicateResourceException("User", "email", request.email)

        if (request.role == Role.SUPER_ADMIN)
            throw AccessDeniedException("Role SUPER_ADMIN não pode ser atribuída via painel.")

        val tempPassword = UUID.randomUUID().toString().replace("-", "").take(12)
        val user = userRepository.save(
            User(
                organization = organization,
                name = request.name,
                email = request.email,
                passwordHash = passwordEncoder.encode(tempPassword),
                role = request.role,
                phoneNumber = request.phoneNumber,
                status = UserStatus.PENDING_VERIFICATION
            )
        )
        return userMapper.toResponse(user)
    }

    @Transactional(readOnly = true)
    fun listOrganizationUsers(): List<UserSummaryResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return userRepository.findAllByOrganizationId(orgId)
            .map { user -> userMapper.toSummary(user) }
    }

    @Transactional(readOnly = true)
    fun findById(userId: UUID): UserResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return userMapper.toResponse(findUserInOrg(userId, orgId))
    }

    @Transactional
    fun updateUserRole(userId: UUID, request: UpdateUserRoleRequest): UserResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val user = findUserInOrg(userId, orgId)
        if (request.role == Role.SUPER_ADMIN)
            throw AccessDeniedException("Role SUPER_ADMIN não pode ser atribuída via painel.")
        user.role = request.role
        return userMapper.toResponse(userRepository.save(user))
    }

    @Transactional
    fun suspendUser(userId: UUID): UserResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val currentUser = SecurityUtils.getCurrentUser()
        if (currentUser.id == userId)
            throw InvalidOperationException("Você não pode suspender sua própria conta.")
        val user = findUserInOrg(userId, orgId)
        if (user.status == UserStatus.SUSPENDED)
            throw InvalidOperationException("Usuário já está suspenso.")
        user.status = UserStatus.SUSPENDED
        return userMapper.toResponse(userRepository.save(user))
    }

    @Transactional
    fun reactivateUser(userId: UUID): UserResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val user = findUserInOrg(userId, orgId)
        if (user.status == UserStatus.ACTIVE)
            throw InvalidOperationException("Usuário já está ativo.")
        user.status = UserStatus.ACTIVE
        return userMapper.toResponse(userRepository.save(user))
    }

    @Transactional
    fun deleteUser(userId: UUID) {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val currentUser = SecurityUtils.getCurrentUser()
        if (currentUser.id == userId)
            throw InvalidOperationException("Você não pode remover sua própria conta.")
        val user = findUserInOrg(userId, orgId)
        userRepository.delete(user)
    }

    private fun findUserInOrg(userId: UUID, orgId: UUID): User {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User", userId) }
        if (!user.belongsToOrganization(orgId))
            throw AccessDeniedException("Usuário não pertence a esta organização.")
        return user
    }
}