package com.astro.leadflow.shared.modules.saas.service

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.iam.domain.User
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.modules.saas.api.requests.AbacatePayWebhookEvent
import com.astro.leadflow.shared.modules.saas.api.requests.CreateSubscriptionCheckoutRequest
import com.astro.leadflow.shared.modules.saas.api.responses.SubscriptionCheckoutResponse
import com.astro.leadflow.shared.modules.saas.api.responses.SubscriptionResponse
import com.astro.leadflow.shared.modules.saas.domain.BillingHistoryEntry
import com.astro.leadflow.shared.modules.saas.domain.BillingHistoryEntryRepository
import com.astro.leadflow.shared.modules.saas.domain.BillingCycle
import com.astro.leadflow.shared.modules.saas.domain.Plan
import com.astro.leadflow.shared.modules.saas.domain.PlanRepository
import com.astro.leadflow.shared.modules.saas.domain.Subscription
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionStatus
import com.astro.leadflow.shared.modules.saas.infra.AbacateCheckoutItem
import com.astro.leadflow.shared.modules.saas.infra.AbacateCreateCustomerRequest
import com.astro.leadflow.shared.modules.saas.infra.AbacateCreateProductRequest
import com.astro.leadflow.shared.modules.saas.infra.AbacateCreateSubscriptionCheckoutRequest
import com.astro.leadflow.shared.modules.saas.infra.AbacatePayGateway
import com.astro.leadflow.shared.modules.saas.mapper.SaasMapper
import com.astro.leadflow.shared.util.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class AbacatePaySubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val planRepository: PlanRepository,
    private val userRepository: UserRepository,
    private val billingHistoryEntryRepository: BillingHistoryEntryRepository,
    private val abacatePayGateway: AbacatePayGateway,
    private val saasMapper: SaasMapper,
    @Value("\${app.base-url:http://localhost:8080}") private val appBaseUrl: String,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val PAYMENT_PROVIDER = "ABACATEPAY"
        private val EXTERNAL_REFERENCE_REGEX =
            Regex("^leadflow-subscription:([0-9a-fA-F-]+):plan:([0-9a-fA-F-]+)$")
    }

    @Transactional
    fun createCheckout(
        planId: UUID,
        request: CreateSubscriptionCheckoutRequest,
    ): SubscriptionCheckoutResponse {
        val currentUser = resolveCurrentUser()
        val subscription = currentSubscription()
        val plan = planRepository.findById(planId)
            .orElseThrow { ResourceNotFoundException("Plan", planId) }

        if (!plan.isSelfServiceAvailable()) {
            throw BusinessException("Plano '${plan.name}' ainda não está disponível para checkout self-service.")
        }

        val productId = ensureRemoteProduct(plan)
        val customerId = ensureRemoteCustomer(subscription, currentUser)
        val externalReference = buildExternalReference(subscription.id, plan.id)
        val fallbackUrl = buildSettingsUrl()

        val checkout = abacatePayGateway.createSubscriptionCheckout(
            AbacateCreateSubscriptionCheckoutRequest(
                items = listOf(AbacateCheckoutItem(id = productId, quantity = 1)),
                customerId = customerId,
                externalId = externalReference,
                returnUrl = request.returnUrl ?: fallbackUrl,
                completionUrl = request.completionUrl ?: fallbackUrl,
                metadata = mapOf(
                    "organizationId" to subscription.organization.id.toString(),
                    "planId" to plan.id.toString(),
                ),
            )
        )

        subscription.paymentProvider = PAYMENT_PROVIDER
        subscription.abacateCheckoutId = checkout.id
        subscription.abacateCheckoutUrl = checkout.url
        subscription.abacateCheckoutStatus = checkout.status
        subscriptionRepository.save(subscription)

        return SubscriptionCheckoutResponse(
            planId = plan.id,
            planName = plan.displayName(),
            checkoutId = checkout.id,
            checkoutUrl = checkout.url,
        )
    }

    @Transactional
    fun cancelCurrentSubscription(): SubscriptionResponse {
        val subscription = currentSubscription()
        val externalSubscriptionId = subscription.abacateSubscriptionId
            ?: throw BusinessException("Nenhuma assinatura AbacatePay ativa foi encontrada para cancelamento.")

        val cancelled = abacatePayGateway.cancelSubscription(externalSubscriptionId)

        subscription.paymentProvider = PAYMENT_PROVIDER
        subscription.status = SubscriptionStatus.CANCELLED
        subscription.abacateCheckoutStatus = cancelled.status ?: "CANCELLED"
        subscription.abacateCheckoutUrl = null

        return saasMapper.toSubscriptionResponse(subscriptionRepository.save(subscription))
    }

    @Transactional
    fun handleWebhook(event: AbacatePayWebhookEvent) {
        when (event.event) {
            "subscription.completed",
            "subscription.renewed" -> handlePaidSubscriptionEvent(event)
            "subscription.cancelled" -> handleCancelledSubscriptionEvent(event)
            else -> log.debug("Evento AbacatePay ignorado: {}", event.event)
        }
    }

    private fun handlePaidSubscriptionEvent(event: AbacatePayWebhookEvent) {
        val providerEventId = event.id?.takeIf { it.isNotBlank() }
        if (providerEventId != null && billingHistoryEntryRepository.findByProviderEventId(providerEventId).isPresent) {
            log.info("Evento AbacatePay já processado: {}", providerEventId)
            return
        }

        val subscription = resolveLocalSubscription(event) ?: run {
            log.warn("Não foi possível correlacionar webhook AbacatePay com assinatura local. event={}", event.id)
            return
        }

        val plan = resolvePlanFromWebhook(event) ?: subscription.plan
        val webhookData = event.data
        val payment = webhookData?.payment
        val checkout = webhookData?.checkout
        val remoteSubscription = webhookData?.subscription

        subscription.plan = plan
        subscription.paymentProvider = PAYMENT_PROVIDER
        subscription.abacateCustomerId = webhookData?.customer?.id ?: subscription.abacateCustomerId
        subscription.abacateSubscriptionId = remoteSubscription?.id ?: subscription.abacateSubscriptionId
        subscription.abacateCheckoutId = checkout?.id ?: subscription.abacateCheckoutId
        subscription.abacateCheckoutStatus = checkout?.status ?: payment?.status ?: subscription.abacateCheckoutStatus
        subscription.abacateCheckoutUrl = null
        subscription.status = when (remoteSubscription?.status) {
            "CANCELLED" -> SubscriptionStatus.CANCELLED
            else -> SubscriptionStatus.ACTIVE
        }
        subscription.renewsAt = computeNextRenewalDate(
            isoDate = remoteSubscription?.updatedAt ?: payment?.updatedAt ?: remoteSubscription?.createdAt,
            billingCycle = BillingCycle.fromApiValue(remoteSubscription?.frequency ?: plan.billingCycle.name),
        )

        val savedSubscription = subscriptionRepository.save(subscription)

        if (payment?.id != null || providerEventId != null) {
            billingHistoryEntryRepository.save(
                BillingHistoryEntry(
                    subscription = savedSubscription,
                    providerEventId = providerEventId,
                    providerPaymentId = payment?.id,
                    providerCheckoutId = checkout?.id,
                    providerSubscriptionId = remoteSubscription?.id,
                    externalReference = payment?.externalId ?: checkout?.externalId,
                    description = buildBillingDescription(plan, parseIsoDate(payment?.updatedAt) ?: parseIsoDate(remoteSubscription?.updatedAt)),
                    amountBrl = centsToBrl(payment?.paidAmount ?: payment?.amount ?: remoteSubscription?.amount ?: checkout?.amount),
                    status = toLocalBillingStatus(payment?.status ?: checkout?.status ?: remoteSubscription?.status),
                    billedAt = parseIsoDate(payment?.createdAt)
                        ?: parseIsoDate(remoteSubscription?.createdAt)
                        ?: parseIsoDate(remoteSubscription?.updatedAt)
                        ?: java.time.LocalDate.now(ZoneOffset.UTC),
                    issuedAt = parseIsoDate(payment?.updatedAt)
                        ?: parseIsoDate(payment?.createdAt)
                        ?: java.time.LocalDate.now(ZoneOffset.UTC),
                    invoiceUrl = payment?.receiptUrl,
                )
            )
        }
    }

    private fun handleCancelledSubscriptionEvent(event: AbacatePayWebhookEvent) {
        val subscription = resolveLocalSubscription(event) ?: run {
            log.warn("Webhook de cancelamento AbacatePay sem correlação local. event={}", event.id)
            return
        }

        subscription.paymentProvider = PAYMENT_PROVIDER
        subscription.status = SubscriptionStatus.CANCELLED
        subscription.abacateSubscriptionId = event.data?.subscription?.id ?: subscription.abacateSubscriptionId
        subscription.abacateCheckoutStatus = "CANCELLED"
        subscription.abacateCheckoutUrl = null
        subscription.renewsAt = null
        subscriptionRepository.save(subscription)
    }

    private fun resolveLocalSubscription(event: AbacatePayWebhookEvent): Subscription? {
        val providerSubscriptionId = event.data?.subscription?.id
        if (!providerSubscriptionId.isNullOrBlank()) {
            val existing = subscriptionRepository.findByAbacateSubscriptionId(providerSubscriptionId).orElse(null)
            if (existing != null) {
                return existing
            }
        }

        val externalReference = event.data?.payment?.externalId ?: event.data?.checkout?.externalId
        val resolvedReference = parseExternalReference(externalReference) ?: return null

        return subscriptionRepository.findById(resolvedReference.subscriptionId).orElse(null)
    }

    private fun resolvePlanFromWebhook(event: AbacatePayWebhookEvent): Plan? {
        val externalReference = event.data?.payment?.externalId ?: event.data?.checkout?.externalId
        val resolvedReference = parseExternalReference(externalReference) ?: return null
        return planRepository.findById(resolvedReference.planId).orElse(null)
    }

    private fun ensureRemoteProduct(plan: Plan): String {
        if (!plan.abacateProductId.isNullOrBlank()) {
            return plan.abacateProductId!!
        }

        val externalId = plan.abacateProductExternalId ?: "leadflow-plan:${plan.id}"
        val product = abacatePayGateway.createProduct(
            AbacateCreateProductRequest(
                externalId = externalId,
                name = plan.displayName(),
                price = toCents(plan.priceBrl),
                description = plan.description,
                cycle = plan.billingCycle.name,
            )
        )

        plan.abacateProductId = product.id
        plan.abacateProductExternalId = product.externalId ?: externalId
        planRepository.save(plan)

        return product.id
    }

    private fun ensureRemoteCustomer(subscription: Subscription, currentUser: User): String {
        if (!subscription.abacateCustomerId.isNullOrBlank()) {
            return subscription.abacateCustomerId!!
        }

        val organization = currentUser.organization
            ?: throw BusinessException("Usuário atual não está vinculado a uma organização.")

        val customer = abacatePayGateway.createCustomer(
            AbacateCreateCustomerRequest(
                email = currentUser.email,
                name = currentUser.name,
                cellphone = normalizePhone(currentUser.phoneNumber),
                taxId = normalizeDigits(organization.cnpj),
                metadata = mapOf(
                    "organizationId" to organization.id.toString(),
                    "organizationSlug" to organization.slug,
                ),
            )
        )

        subscription.paymentProvider = PAYMENT_PROVIDER
        subscription.abacateCustomerId = customer.id
        subscriptionRepository.save(subscription)

        return customer.id
    }

    private fun currentSubscription(): Subscription {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return subscriptionRepository.findByOrganizationId(orgId)
            .orElseThrow { ResourceNotFoundException("Subscription", orgId) }
    }

    private fun resolveCurrentUser(): User {
        val principal = SecurityUtils.getCurrentUser()
        return userRepository.findByIdWithOrganization(principal.id).orElse(principal)
    }

    private fun buildSettingsUrl(): String = appBaseUrl.trimEnd('/') + "/app/configuracoes"

    private fun buildExternalReference(subscriptionId: UUID, planId: UUID): String =
        "leadflow-subscription:$subscriptionId:plan:$planId"

    private fun parseExternalReference(value: String?): ResolvedExternalReference? {
        if (value.isNullOrBlank()) return null
        val match = EXTERNAL_REFERENCE_REGEX.matchEntire(value) ?: return null
        return runCatching {
            ResolvedExternalReference(
                subscriptionId = UUID.fromString(match.groupValues[1]),
                planId = UUID.fromString(match.groupValues[2]),
            )
        }.getOrNull()
    }

    private fun toCents(value: BigDecimal): Long =
        value.movePointRight(2).longValueExact()

    private fun centsToBrl(value: Long?): BigDecimal =
        BigDecimal.valueOf(value ?: 0L, 2)

    private fun normalizeDigits(value: String?): String? =
        value?.filter(Char::isDigit)?.takeIf { it.isNotBlank() }

    private fun normalizePhone(value: String?): String? {
        val digits = normalizeDigits(value) ?: return null
        val rawValue = value?.trim()
        return when {
            rawValue?.startsWith("+") == true -> rawValue
            digits.startsWith("55") -> "+$digits"
            digits.length in 10..11 -> "+55$digits"
            else -> digits
        }
    }

    private fun computeNextRenewalDate(isoDate: String?, billingCycle: BillingCycle): java.time.LocalDate? {
        val baseDate = parseOffsetDateTime(isoDate)?.toLocalDate() ?: return null
        return billingCycle.nextRenewalFrom(baseDate)
    }

    private fun parseIsoDate(value: String?): java.time.LocalDate? =
        parseOffsetDateTime(value)?.toLocalDate()

    private fun parseOffsetDateTime(value: String?): OffsetDateTime? =
        runCatching { value?.let(OffsetDateTime::parse) }.getOrNull()

    private fun buildBillingDescription(plan: Plan, date: java.time.LocalDate?): String {
        val referenceDate = date ?: java.time.LocalDate.now(ZoneOffset.UTC)
        return "${plan.displayName()} - ${"%02d/%04d".format(referenceDate.monthValue, referenceDate.year)}"
    }

    private fun toLocalBillingStatus(status: String?): String =
        when (status) {
            "PAID", "ACTIVE" -> "PAID"
            "PENDING" -> "PENDING"
            else -> "FAILED"
        }
}

private data class ResolvedExternalReference(
    val subscriptionId: UUID,
    val planId: UUID,
)
