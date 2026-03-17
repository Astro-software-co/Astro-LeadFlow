package com.astro.leadflow.shared.modules.ingestion.service

import com.astro.leadflow.shared.modules.communication.service.MessageSenderService
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.ingestion.infra.SqsProducer
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.ingestion.dto.ScrapedLeadDTO
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

    @Transactional
    fun ingestFromWebhook(dto: ScrapedLeadDTO) {
        val normalizedPhone = normalizationService.normalizePhone(dto.phoneNumber)
        val normalizedMarketplace = normalizationService.normalizeMarketplace(dto.marketplaceType)

        val organization = organizationRepository.findBySlug(dto.organizationSlug)
            .orElseThrow { ResourceNotFoundException("Organization", dto.organizationSlug) }

        val isDuplicate = leadRepository.existsByOrganizationIdAndPhoneNumber(
            organization.id, normalizedPhone
        )

        if (isDuplicate) {
            log.info("Lead duplicado ignorado: {} | org: {}", normalizedPhone, dto.organizationSlug)
            return
        }

        val normalizedLead = dto.copy(
            phoneNumber = normalizedPhone,
            marketplaceType = normalizedMarketplace
        )

        if (awsEnabled) {
            sqsProducer.publishNewLead(normalizedLead)
            return
        }

        log.info("AWS desabilitado. Processando lead localmente: {} | {}", normalizedPhone, dto.organizationSlug)
        processFromQueue(normalizedLead)
    }

    @Transactional
    fun processFromQueue(dto: ScrapedLeadDTO) {
        val organization = organizationRepository.findBySlug(dto.organizationSlug)
            .orElseThrow { ResourceNotFoundException("Organization", dto.organizationSlug) }

        val alreadyExists = leadRepository.existsByOrganizationIdAndPhoneNumber(
            organization.id, dto.phoneNumber
        )
        if (alreadyExists) return

        val lead = leadRepository.save(
            Lead(
                organization = organization,
                phoneNumber = dto.phoneNumber,
                name = dto.name,
                email = dto.email,
                origin = dto.marketplaceType,
                vehicleInterest = dto.vehicleInterest,
                pipelineStage = PipelineStage.TALKING_TO_BOT
            )
        )

        try {
            messageSenderService.sendTemplate(
                lead = lead,
                organization = organization,
                templateName = "welcome_lead",
                marketplaceName = dto.marketplaceType
            )
            log.info("Mensagem de boas-vindas enviada para {}", dto.phoneNumber)
        } catch (e: Exception) {
            log.error("Falha ao enviar mensagem de boas-vindas: {}", e.message)
        }
    }
}