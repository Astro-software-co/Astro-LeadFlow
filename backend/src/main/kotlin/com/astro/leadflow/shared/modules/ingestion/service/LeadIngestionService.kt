package com.astro.leadflow.modules.ingestion.service

import com.astro.leadflow.modules.communication.service.MessageSenderService
import com.astro.leadflow.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.modules.ingestion.api.requests.ScrapedLeadDTO
import com.astro.leadflow.modules.ingestion.infra.SqsProducer
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeadIngestionService(
    private val leadRepository: LeadRepository,
    private val organizationRepository: OrganizationRepository,
    private val normalizationService: NormalizationService,
    private val sqsProducer: SqsProducer,
    private val messageSenderService: MessageSenderService,
    @Value("\${app.aws.enabled:false}") private val awsEnabled: Boolean
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Receives lead from webhook.
     * If AWS is disabled, processes immediately without queue.
     */
    @Transactional
    fun ingestFromWebhook(dto: ScrapedLeadDTO) {
        val normalizedPhone = normalizationService.normalizePhone(dto.phoneNumber)
        val normalizedMarketplace = normalizationService.normalizeMarketplace(dto.marketplace)

        val organization = organizationRepository.findBySlug(dto.organizationSlug)
            .orElseThrow { ResourceNotFoundException("Organization", dto.organizationSlug) }

        val isDuplicate = leadRepository.existsByOrganizationIdAndPhoneNumber(
            organization.id,
            normalizedPhone
        )

        if (isDuplicate) {
            log.info("Duplicate lead ignored: {} | org: {}", normalizedPhone, dto.organizationSlug)
            return
        }

        val normalizedLead = dto.copy(
            phoneNumber = normalizedPhone,
            marketplace = normalizedMarketplace
        )

        if (awsEnabled) {
            sqsProducer.publishNewLead(normalizedLead)
            return
        }

        log.info("AWS disabled. Processing lead locally: {} | {}", normalizedPhone, dto.organizationSlug)
        processFromQueue(normalizedLead)
    }

    /**
     * Consumed from SQS or called directly in local mode.
     */
    @Transactional
    fun processFromQueue(dto: ScrapedLeadDTO) {
        val organization = organizationRepository.findBySlug(dto.organizationSlug)
            .orElseThrow { ResourceNotFoundException("Organization", dto.organizationSlug) }

        val alreadyExists = leadRepository.existsByOrganizationIdAndPhoneNumber(
            organization.id,
            dto.phoneNumber
        )
        if (alreadyExists) return

        val lead = leadRepository.save(
            Lead(
                organization = organization,
                phoneNumber = dto.phoneNumber,
                name = dto.name,
                email = dto.email,
                origin = dto.marketplace,
                vehicleInterest = dto.vehicleInterest,
                pipelineStage = PipelineStage.TALKING_TO_BOT
            )
        )

        try {
            messageSenderService.sendTemplate(
                lead = lead,
                organization = organization,
                templateName = "welcome_lead",
                marketplaceName = dto.marketplace
            )
            log.info("Marketing message sent to {}", dto.phoneNumber)
        } catch (e: Exception) {
            log.error("Failed to send marketing message: {}", e.message)
        }
    }
}
