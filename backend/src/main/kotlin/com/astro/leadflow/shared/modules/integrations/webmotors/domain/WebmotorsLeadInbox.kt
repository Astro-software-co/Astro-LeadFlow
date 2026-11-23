package com.astro.leadflow.shared.modules.integrations.webmotors.domain

import jakarta.persistence.*
import com.astro.leadflow.shared.modules.iam.domain.Organization
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Proposta recebida da Webmotors. Existe separada de [com.astro.leadflow.shared.modules.crm.domain.Lead]
 * porque o lead pode estar pendente de aceite e sem telefone completo, e porque guarda detalhes
 * específicos da Webmotors sem poluir o domínio do CRM. Não armazena dados de contato do cliente.
 */
@Entity
@Table(
    name = "webmotors_lead_inbox",
    uniqueConstraints = [UniqueConstraint(columnNames = ["organization_id", "external_id", "lead_type"])],
    indexes = [Index(name = "idx_wm_inbox_org_status", columnList = "organization_id,status")]
)
class WebmotorsLeadInbox(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: WebmotorsAccount,

    @Column(name = "external_id", nullable = false, length = 100)
    val externalId: String,

    /** `tipoLead` da Webmotors; 0 quando não informado. */
    @Column(name = "lead_type", nullable = false)
    val leadType: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: InboxStatus = InboxStatus.PENDING,

    @Column(columnDefinition = "TEXT")
    var message: String? = null,

    @Column(name = "channel", length = 100)
    var channel: String? = null,

    @Column(name = "type_description", length = 255)
    var typeDescription: String? = null,

    @Column(name = "remote_status", length = 100)
    var remoteStatus: String? = null,

    @Column(name = "remote_created_at", length = 100)
    var remoteCreatedAt: String? = null,

    @Column(name = "ad_id", length = 100)
    var adId: String? = null,

    @Column(name = "ad_brand", length = 100)
    var adBrand: String? = null,

    @Column(name = "ad_model", length = 150)
    var adModel: String? = null,

    @Column(name = "ad_version", length = 255)
    var adVersion: String? = null,

    @Column(name = "ad_year")
    var adYear: Int? = null,

    @Column(name = "ad_plate", length = 20)
    var adPlate: String? = null,

    @Column(name = "ad_price", precision = 12, scale = 2)
    var adPrice: BigDecimal? = null,

    /** Item como veio da listagem (sem contato), para auditoria e depuração do formato real. */
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    var rawPayload: String? = null,

    @Column(name = "refusal_code")
    var refusalCode: Int? = null,

    @Column(name = "refusal_message", length = 500)
    var refusalMessage: String? = null,

    /** Lead criado no CRM (sem FK de JPA para manter o módulo desacoplado; há FK no SQL). */
    @Column(name = "lead_id")
    var leadId: UUID? = null,

    @Column(nullable = false)
    var attempts: Int = 0,

    @Column(name = "last_error", length = 500)
    var lastError: String? = null,

    @Column(name = "received_at", nullable = false, updatable = false)
    val receivedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "decided_at")
    var decidedAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onUpdate() { updatedAt = LocalDateTime.now() }
}
