package com.astro.leadflow.shared.modules.communication.meta

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.UUID
import com.astro.leadflow.shared.util.PiiMasker

@Component
@ConditionalOnProperty(prefix = "meta.api", name = ["mode"], havingValue = "mock", matchIfMissing = true)
class MockMetaApiClient : MetaApiClient {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun sendText(
        phoneNumberId: String,
        accessToken: String,
        toPhone: String,
        message: String
    ): String {
        val id = "mock-wa-${UUID.randomUUID()}"
        log.info("Mock Meta sendText phoneNumberId={} to={} messageId={}", phoneNumberId, PiiMasker.maskPhone(toPhone), id)
        return id
    }

    override fun sendTemplate(
        phoneNumberId: String,
        accessToken: String,
        toPhone: String,
        templateName: String,
        languageCode: String,
        components: List<Map<String, Any>>
    ): String {
        val id = "mock-wa-${UUID.randomUUID()}"
        log.info("Mock Meta sendTemplate phoneNumberId={} to={} template={} messageId={}", phoneNumberId, PiiMasker.maskPhone(toPhone), templateName, id)
        return id
    }
}
