package com.astro.leadflow.shared.modules.ingestion.service

import com.astro.leadflow.shared.modules.communication.service.MessageSenderService
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.ingestion.infra.SqsProducer
import com.astro.leadflow.shared.modules.saas.service.UsageLimitService
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.exception.ExternalServiceException
import com.astro.leadflow.shared.exception.QuotaExceededException
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
    private val usageLimitService: UsageLimitService,
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
            log.info("Lead duplicado recebido: {} | org: {}", normalizedPhone, dto.organizationSlug)
            val duplicateLead = leadRepository.save(
                Lead(
                    organization = organization,
                    phoneNumber = normalizedPhone,
                    name = dto.name,
                    email = dto.email,
                    origin = normalizationService.normalizeMarketplace(dto.marketplaceType),
                    vehicleInterest = dto.vehicleInterest,
                    pipelineStage = PipelineStage.TALKING_TO_BOT,
                    isDuplicate = true
                )
            )

            usageLimitService.incrementUsage(organization.id, normalizedMarketplace)

            try {
                messageSenderService.sendTemplate(
                    lead = duplicateLead,
                    organization = organization,
                    templateName = "welcome_lead",
                    marketplaceName = dto.marketplaceType
                )
            } catch (e: Exception) {
                log.warn("Erro ao enviar WhatsApp para lead duplicado. leadId={} error={}", duplicateLead.id, e.message)
            }
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
        if (alreadyExists) {
            log.debug("Lead já existe na fila. phone={} org={}", dto.phoneNumber, dto.organizationSlug)
            return
        }

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

        log.info("Lead salvo com sucesso. id={} phone={} org={}", lead.id, dto.phoneNumber, dto.organizationSlug)

        try {
            messageSenderService.sendTemplate(
                lead = lead,
                organization = organization,
                templateName = "welcome_lead",
                marketplaceName = dto.marketplaceType
            )
            log.info("Mensagem de boas-vindas enviada para {}", dto.phoneNumber)
        } catch (e: QuotaExceededException) {
            log.warn(
                "Quota excedida ao enviar boas-vindas. Lead salvo sem mensagem. leadId={} org={} marketplace={}",
                lead.id, dto.organizationSlug, dto.marketplaceType
            )
        } catch (e: ExternalServiceException) {
            log.error(
                "Erro Meta API ao enviar boas-vindas. Lead salvo sem mensagem. leadId={} error={}",
                lead.id, e.message
            )
        } catch (e: Exception) {
            log.error(
                "Erro inesperado ao enviar boas-vindas. Lead salvo sem mensagem. leadId={} error={}",
                lead.id, e.message, e
            )
        }
    }
}