package com.astro.leadflow.shared.modules.integrations.webmotors.domain

import com.astro.leadflow.shared.modules.iam.domain.Organization
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class AcceptMode {
    /** O lojista aceita/recusa cada proposta pela tela. Padrão (mais seguro). */
    MANUAL,

    /** O sistema aceita todas as propostas assim que chegam. */
    AUTO
}

/** Credenciais e preferências da integração Webmotors de uma loja (1 por organização). */
@Entity
@Table(
    name = "webmotors_accounts",
    uniqueConstraints = [UniqueConstraint(columnNames = ["organization_id"])]
)
class WebmotorsAccount(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    /** ID/CNPJ da loja na Webmotors (`loja.id`), usado para conferir os leads recebidos. */
    @Column(name = "store_id")
    var storeId: String? = null,

    @Column(name = "client_id_enc", nullable = false, length = 1000)
    var clientIdEnc: String,

    @Column(name = "client_secret_enc", nullable = false, length = 1000)
    var clientSecretEnc: String,

    @Column(name = "username_enc", nullable = false, length = 1000)
    var usernameEnc: String,

    @Column(name = "password_enc", nullable = false, length = 1000)
    var passwordEnc: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "accept_mode", nullable = false, length = 20)
    var acceptMode: AcceptMode = AcceptMode.MANUAL,

    /** Envia o template `welcome_lead` por WhatsApp ao importar um lead novo. Desligado até a base legal ser definida. */
    @Column(name = "welcome_message_enabled", nullable = false)
    var welcomeMessageEnabled: Boolean = false,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "last_sync_at")
    var lastSyncAt: LocalDateTime? = null,

    @Column(name = "last_sync_ok")
    var lastSyncOk: Boolean? = null,

    @Column(name = "last_sync_error", length = 500)
    var lastSyncError: String? = null,

    /** Reserva de sincronização: enquanto no futuro, nenhuma outra instância sincroniza esta conta. */
    @Column(name = "lease_until")
    var leaseUntil: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onUpdate() { updatedAt = LocalDateTime.now() }
}
