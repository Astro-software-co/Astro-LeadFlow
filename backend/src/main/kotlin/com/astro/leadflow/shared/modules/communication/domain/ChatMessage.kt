package com.astro.leadflow.shared.modules.communication.domain

import com.astro.leadflow.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.crm.domain.Lead
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "chat_messages")
class ChatMessage(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    val lead: Lead,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val direction: MessageDirection,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    val messageType: MessageType = MessageType.TEXT,

    @Column(name = "wa_message_id")
    var waMessageId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MessageStatus = MessageStatus.SENT,

    @Column(name = "sent_at", nullable = false, updatable = false)
    val sentAt: LocalDateTime = LocalDateTime.now()
)

enum class MessageDirection { INBOUND, OUTBOUND }
enum class MessageType { TEXT, IMAGE, DOCUMENT, TEMPLATE }
enum class MessageStatus { SENT, DELIVERED, READ, FAILED }