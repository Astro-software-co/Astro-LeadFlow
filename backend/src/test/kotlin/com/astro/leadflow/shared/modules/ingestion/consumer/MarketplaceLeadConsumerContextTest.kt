package com.astro.leadflow.shared.modules.ingestion.consumer

import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceWebhookEventRepository
import com.astro.leadflow.shared.modules.ingestion.service.LeadIngestionService
import com.astro.leadflow.shared.modules.ingestion.service.LeadNormalizationService
import com.astro.leadflow.shared.modules.ingestion.service.marketplace.MarketplaceLeadFetcher
import com.astro.leadflow.shared.modules.ingestion.service.marketplace.MercadoLivreAuthService
import com.astro.leadflow.shared.modules.ingestion.service.marketplace.MercadoLivreLeadFetcher
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredentialRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.web.reactive.function.client.WebClient

/** Após a remoção do legado Webmotors (T1.6), o único fetcher de marketplace é o do Mercado Livre. */
class MarketplaceLeadConsumerContextTest {

    @Test
    fun `o unico fetcher de marketplace no codigo e o do Mercado Livre`() {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
            .apply { addIncludeFilter(AssignableTypeFilter(MarketplaceLeadFetcher::class.java)) }

        val fetchers = scanner.findCandidateComponents("com.astro.leadflow").map { it.beanClassName }

        assertEquals(listOf(MercadoLivreLeadFetcher::class.qualifiedName), fetchers)
    }

    @Test
    fun `consumer sobe com AWS ligada e so o fetcher do Mercado Livre`() {
        ApplicationContextRunner()
            .withPropertyValues("app.aws.enabled=true")
            .withBean(ObjectMapper::class.java, { ObjectMapper() })
            .withBean(MercadoLivreLeadFetcher::class.java, {
                MercadoLivreLeadFetcher(mockk<MercadoLivreAuthService>(), ObjectMapper(), WebClient.builder(), "http://localhost")
            })
            .withBean(MarketplaceCredentialRepository::class.java, { mockk() })
            .withBean(LeadNormalizationService::class.java, { mockk() })
            .withBean(LeadIngestionService::class.java, { mockk() })
            .withBean(MarketplaceWebhookEventRepository::class.java, { mockk() })
            .withUserConfiguration(MarketplaceLeadConsumer::class.java)
            .run { context ->
                assertEquals(1, context.getBeansOfType(MarketplaceLeadConsumer::class.java).size)
                assertEquals(1, context.getBeansOfType(MarketplaceLeadFetcher::class.java).size)
            }
    }
}
