package com.astro.leadflow.shared.modules.ingestion.service

import com.astro.leadflow.shared.modules.communication.service.MessageSenderService
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.ingestion.infra.SqsProducer
import com.astro.leadflow.shared.modules.saas.service.UsageLimitService
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.modules.crm.intake.LeadIntakeCommand
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.exception.ExternalServiceException
import com.astro.leadflow.shared.exception.QuotaExceededException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.ingestion.dto.ScrapedLeadDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.astro.leadflow.shared.util.PiiMasker

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

        // Duplicado é tratado já na entrada (não passa pela fila), como sempre foi.
        if (leadRepository.existsByOrganizationIdAndPhoneNumber(organization.id, normalizedPhone)) {
            ingestResolved(organization, dto.toCommand(organization, normalizedPhone, normalizedMarketplace))
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

        log.info("AWS desabilitado. Processando lead localmente: {} | {}", PiiMasker.maskPhone(normalizedPhone), dto.organizationSlug)
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
            log.debug("Lead já existe na fila. phone={} org={}", PiiMasker.maskPhone(dto.phoneNumber), dto.organizationSlug)
            return
        }

        ingestResolved(organization, dto.toCommand(organization, dto.phoneNumber, dto.marketplaceType))
    }

    /**
     * Ponto único de criação de lead a partir de uma entrada já resolvida para a organização:
     * normaliza, cria (telefone repetido → `isDuplicate=true`, ainda com mensagem) e envia a
     * abordagem quando pedida. Falha de cota ou da Meta não desfaz o lead.
     */
    @Transactional
    fun ingestResolved(organization: Organization, command: LeadIntakeCommand): Lead {
        val phone = normalizationService.normalizePhone(
            requireNotNull(command.phone) { "Lead sem telefone." }
        )
        val marketplace = normalizationService.normalizeMarketplace(command.origin)
        val duplicate = leadRepository.existsByOrganizationIdAndPhoneNumber(organization.id, phone)
        if (duplicate) {
            log.info("Lead duplicado recebido: {} | org: {}", PiiMasker.maskPhone(phone), organization.slug)
        }

        val lead = leadRepository.save(
            Lead(
                organization = organization,
                phoneNumber = phone,
                name = command.name,
                email = command.email,
                origin = marketplace,
                vehicleInterest = command.vehicleInterest ?: command.vehicleSnapshot?.describe(),
                notes = command.message,
                pipelineStage = if (command.sendWelcome) PipelineStage.TALKING_TO_BOT else PipelineStage.WAITING_ATTENDANCE,
                isDuplicate = duplicate
            )
        )
        if (!duplicate) {
            log.info("Lead salvo com sucesso. id={} phone={} org={}", lead.id, PiiMasker.maskPhone(phone), organization.slug)
        }
        if (!command.sendWelcome) return lead

        if (duplicate) {
            // Contagem explícita fixada por LeadIngestionServiceTest; sendTemplate também conta (ver design da change webmotors-leads-polling).
            usageLimitService.incrementUsage(organization.id, marketplace)
            try {
                messageSenderService.sendTemplate(
                    lead = lead,
                    organization = organization,
                    templateName = "welcome_lead",
                    marketplaceName = marketplace
                )
            } catch (e: Exception) {
                log.warn("Erro ao enviar WhatsApp para lead duplicado. leadId={} error={}", lead.id, e.message)
            }
            return lead
        }

        try {
            messageSenderService.sendTemplate(
                lead = lead,
                organization = organization,
                templateName = "welcome_lead",
                marketplaceName = marketplace
            )
            log.info("Mensagem de boas-vindas enviada. leadId={}", lead.id)
        } catch (e: QuotaExceededException) {
            log.warn(
                "Quota excedida ao enviar boas-vindas. Lead salvo sem mensagem. leadId={} org={} marketplace={}",
                lead.id, organization.slug, marketplace
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
        return lead
    }

    private fun ScrapedLeadDTO.toCommand(organization: Organization, phone: String, marketplace: String) =
        LeadIntakeCommand(
            organizationId = organization.id,
            name = name,
            phone = phone,
            email = email,
            origin = marketplace,
            externalEventId = null,
            vehicleInterest = vehicleInterest
        )
}
