package com.astro.leadflow.modules.ingestion.service

import com.astro.leadflow.modules.communication.service.MessageSenderService
import com.astro.leadflow.modules.crm.domain.Lead
import com.astro.leadflow.modules.crm.domain.LeadRepository
import com.astro.leadflow.modules.crm.domain.PipelineStage
import com.astro.leadflow.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.modules.ingestion.api.requests.ScrapedLeadDTO
import com.astro.leadflow.modules.ingestion.infra.SqsProducer
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeadIngestionService(
    private val leadRepository: LeadRepository,
    private val organizationRepository: OrganizationRepository,
    private val normalizationService: NormalizationService,
    private val sqsProducer: SqsProducer,
    private val messageSenderService: MessageSenderService
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Recebe lead do ScraperWebhookController (POST direto dos robôs).
     * Normaliza, valida duplicidade e publica na fila SQS.
     */
    @Transactional
    fun ingestFromWebhook(dto: ScrapedLeadDTO) {
        val normalizedPhone = normalizationService.normalizePhone(dto.phoneNumber)
        val normalizedMarketplace = normalizationService.normalizeMarketplace(dto.marketplace)

        val organization = organizationRepository.findBySlug(dto.organizationSlug)
            .orElseThrow { ResourceNotFoundException("Organization", dto.organizationSlug) }

        val isDuplicate = leadRepository.existsByOrganizationIdAndPhoneNumber(
            organization.id, normalizedPhone
        )

        if (isDuplicate) {
            log.info("Lead duplicado ignorado: $normalizedPhone | org: ${dto.organizationSlug}")
            return
        }

        // Publica na fila — processamento assíncrono
        sqsProducer.publishNewLead(
            dto.copy(
                phoneNumber = normalizedPhone,
                marketplace = normalizedMarketplace
            )
        )
    }

    /**
     * Consumido pelo SqsConsumer — persiste lead e dispara mensagem de marketing.
     */
    @Transactional
    fun processFromQueue(dto: ScrapedLeadDTO) {
        val organization = organizationRepository.findBySlug(dto.organizationSlug)
            .orElseThrow { ResourceNotFoundException("Organization", dto.organizationSlug) }

        // Dupla checagem de duplicidade (pode ter chegado mais de uma msg na fila)
        val alreadyExists = leadRepository.existsByOrganizationIdAndPhoneNumber(
            organization.id, dto.phoneNumber
        )
        if (alreadyExists) return

        // Persiste o lead
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

        // Dispara mensagem de marketing (template aprovado pela Meta)
        try {
            messageSenderService.sendTemplate(
                lead = lead,
                organization = organization,
                templateName = "welcome_lead",
                marketplaceName = dto.marketplace
            )
            log.info("Mensagem de marketing enviada para ${dto.phoneNumber}")
        } catch (e: Exception) {
            // Não falha a transação se o envio falhar — lead já está salvo
            log.error("Falha ao enviar mensagem de marketing: ${e.message}")
        }
    }
}