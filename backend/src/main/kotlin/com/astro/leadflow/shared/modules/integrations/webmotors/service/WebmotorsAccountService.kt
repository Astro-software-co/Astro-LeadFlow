package com.astro.leadflow.shared.modules.integrations.webmotors.service

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.ExternalServiceException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.WebmotorsAccount
import com.astro.leadflow.shared.modules.integrations.webmotors.domain.WebmotorsAccountRepository
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApi
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WebmotorsApiException
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.WmCredentials
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class WebmotorsAccountService(
    private val accountRepository: WebmotorsAccountRepository,
    private val organizationRepository: OrganizationRepository,
    private val cipher: SecretCipher,
    private val api: WebmotorsApi
) {

    @Transactional
    fun upsert(organizationId: UUID, cmd: AccountCommand): AccountView {
        val existing = accountRepository.findByOrganizationId(organizationId).orElse(null)

        val account = if (existing == null) {
            val org = organizationRepository.findById(organizationId)
                .orElseThrow { ResourceNotFoundException("Organização", organizationId) }
            val missing = listOf(
                "clientId" to cmd.clientId, "clientSecret" to cmd.clientSecret,
                "username" to cmd.username, "password" to cmd.password
            ).filter { it.second.isNullOrBlank() }.map { it.first }
            if (missing.isNotEmpty())
                throw BusinessException("Campos obrigatórios para configurar a Webmotors: ${missing.joinToString()}.")

            WebmotorsAccount(
                organization = org,
                clientIdEnc = cipher.encrypt(cmd.clientId!!.trim()),
                clientSecretEnc = cipher.encrypt(cmd.clientSecret!!.trim()),
                usernameEnc = cipher.encrypt(cmd.username!!.trim()),
                passwordEnc = cipher.encrypt(cmd.password!!)
            )
        } else existing.also { a ->
            cmd.clientId?.takeIf { it.isNotBlank() }?.let { a.clientIdEnc = cipher.encrypt(it.trim()) }
            cmd.clientSecret?.takeIf { it.isNotBlank() }?.let { a.clientSecretEnc = cipher.encrypt(it.trim()) }
            cmd.username?.takeIf { it.isNotBlank() }?.let { a.usernameEnc = cipher.encrypt(it.trim()) }
            cmd.password?.takeIf { it.isNotBlank() }?.let { a.passwordEnc = cipher.encrypt(it) }
        }

        cmd.storeId?.let { account.storeId = it.trim().ifBlank { null } }
        cmd.acceptMode?.let { account.acceptMode = it }
        cmd.welcomeMessageEnabled?.let { account.welcomeMessageEnabled = it }
        cmd.enabled?.let { account.enabled = it }

        return view(accountRepository.save(account))
    }

    @Transactional(readOnly = true)
    fun view(organizationId: UUID): AccountView? =
        accountRepository.findByOrganizationId(organizationId).map { view(it) }.orElse(null)

    /** Valida as credenciais fazendo login na Webmotors. */
    fun testConnection(organizationId: UUID) {
        val runtime = runtimeOfOrganization(organizationId)
        try {
            api.login(runtime.credentials)
        } catch (e: WebmotorsApiException) {
            throw ExternalServiceException("Webmotors", e.message ?: "falha no login")
        }
    }

    @Transactional(readOnly = true)
    fun runtimeOf(accountId: UUID): AccountRuntime =
        accountRepository.findById(accountId)
            .orElseThrow { ResourceNotFoundException("Conta Webmotors", accountId) }
            .let(::runtime)

    @Transactional(readOnly = true)
    fun runtimeOfOrganization(organizationId: UUID): AccountRuntime =
        accountRepository.findByOrganizationId(organizationId)
            .orElseThrow { ResourceNotFoundException("Conta Webmotors da organização", organizationId) }
            .let(::runtime)

    @Transactional(readOnly = true)
    fun enabledAccountIds(): List<UUID> = accountRepository.findAllByEnabledTrue().map { it.id }

    @Transactional
    fun recordSync(accountId: UUID, ok: Boolean, error: String?) {
        accountRepository.findById(accountId).ifPresent {
            it.lastSyncAt = LocalDateTime.now()
            it.lastSyncOk = ok
            it.lastSyncError = error?.take(500)
        }
    }

    private fun runtime(a: WebmotorsAccount) = AccountRuntime(
        id = a.id,
        organizationId = a.organization.id,
        storeId = a.storeId,
        acceptMode = a.acceptMode,
        welcomeMessageEnabled = a.welcomeMessageEnabled,
        credentials = WmCredentials(
            accountId = a.id,
            clientId = cipher.decrypt(a.clientIdEnc),
            clientSecret = cipher.decrypt(a.clientSecretEnc),
            username = cipher.decrypt(a.usernameEnc),
            password = cipher.decrypt(a.passwordEnc)
        )
    )

    private fun view(a: WebmotorsAccount) = AccountView(
        id = a.id,
        storeId = a.storeId,
        clientIdHint = cipher.decrypt(a.clientIdEnc).let { "***" + it.takeLast(4) },
        acceptMode = a.acceptMode,
        welcomeMessageEnabled = a.welcomeMessageEnabled,
        enabled = a.enabled,
        lastSyncAt = a.lastSyncAt,
        lastSyncOk = a.lastSyncOk,
        lastSyncError = a.lastSyncError
    )
}
