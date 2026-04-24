package com.astro.leadflow.shared.modules.iam.domain

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = true)
    var organization: Organization? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    private var passwordHash: String,

    @Column(name = "phone_number")
    var phoneNumber: String? = null,

    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.SELLER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "two_factor_email_enabled", nullable = false)
    var twoFactorEmailEnabled: Boolean = false,

    @Column(name = "two_factor_sms_enabled", nullable = false)
    var twoFactorSmsEnabled: Boolean = false,

    @Column(name = "two_factor_code")
    var twoFactorCode: String? = null,

    @Column(name = "two_factor_code_expires_at")
    var twoFactorCodeExpiresAt: LocalDateTime? = null,

    @Column(name = "notify_new_lead", nullable = false)
    var notifyNewLead: Boolean = true,

    @Column(name = "notify_lead_waiting", nullable = false)
    var notifyLeadWaiting: Boolean = true,

    @Column(name = "notify_sale_completed", nullable = false)
    var notifySaleCompleted: Boolean = true,

    @Column(name = "notify_card_moved", nullable = false)
    var notifyCardMoved: Boolean = false,

    @Column(name = "notify_via_email", nullable = false)
    var notifyViaEmail: Boolean = false,

    @Column(name = "notify_via_push", nullable = false)
    var notifyViaPush: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : UserDetails {

    @PreUpdate
    fun onUpdate() { updatedAt = LocalDateTime.now() }

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val base = SimpleGrantedAuthority("ROLE_${role.name}")
        return when (role) {
            Role.ADMIN, Role.MANAGER, Role.SELLER ->
                listOf(base, SimpleGrantedAuthority("ROLE_DEALERSHIP"))
            Role.ASTRO_ADMIN ->
                listOf(SimpleGrantedAuthority("ROLE_ASTRO_ADMIN"))
            Role.SUPER_ADMIN ->
                listOf(SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        }
    }

    override fun getPassword(): String = passwordHash
    override fun getUsername(): String = email
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = status != UserStatus.SUSPENDED
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = status == UserStatus.ACTIVE

    fun updatePassword(newHash: String) { this.passwordHash = newHash }

    fun belongsToOrganization(organizationId: UUID): Boolean =
        organization?.id == organizationId
}