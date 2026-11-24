package com.astro.leadflow.shared.modules.communication.service

import com.astro.leadflow.shared.exception.RecipientBlockedException
import com.astro.leadflow.shared.modules.communication.domain.ChatMessageRepository
import com.astro.leadflow.shared.modules.communication.meta.MetaApiClient
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.saas.service.UsageLimitService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/** T2.2: lead anonimizado não recebe nenhuma mensagem do sistema (template, texto ou bot). */
class MessageSenderRecipientBlockTest {

    private val meta = mockk<MetaApiClient>()
    private val sender = MessageSenderService(meta, mockk<ChatMessageRepository>(), mockk<UsageLimitService>(), mockk(relaxed = true), "mock", "", "")
    private val org = Organization(name = "Loja", slug = "loja")
    private val anonymized = Lead(
        organization = org, phoneNumber = "ANON-1a2b3c4d", origin = "WEBMOTORS", anonymizedAt = LocalDateTime.now()
    )

    @Test
    fun `template e texto sao recusados antes de chegar a Meta`() {
        assertThrows<RecipientBlockedException> { sender.sendTemplate(anonymized, org, "welcome_lead") }
        assertThrows<RecipientBlockedException> { sender.sendText(anonymized, org, "oi") }
        verify(exactly = 0) { meta.sendTemplate(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { meta.sendText(any(), any(), any(), any()) }
    }
}
