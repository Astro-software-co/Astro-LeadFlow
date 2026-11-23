package com.astro.leadflow.shared.modules.admin.service

import com.astro.leadflow.shared.modules.admin.api.responses.AdminDashboardMetricsResponse
import com.astro.leadflow.shared.modules.admin.api.responses.AdminDashboardResponse
import com.astro.leadflow.shared.modules.admin.api.responses.AdminPlanRevenueResponse
import com.astro.leadflow.shared.modules.admin.api.responses.AdminProjectionResponse
import com.astro.leadflow.shared.modules.admin.api.responses.AdminRevenuePointResponse
import com.astro.leadflow.shared.modules.admin.api.responses.AdminSubscriberPointResponse
import com.astro.leadflow.shared.modules.admin.api.responses.AdminSubscriptionMovementPointResponse
import com.astro.leadflow.shared.modules.admin.api.responses.AdminSubscriptionRowResponse
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingApplicationRepository
import com.astro.leadflow.shared.modules.saas.domain.BillingHistoryEntry
import com.astro.leadflow.shared.modules.saas.domain.BillingHistoryEntryRepository
import com.astro.leadflow.shared.modules.saas.domain.Subscription
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

@Service
class AdminDashboardService(
    private val subscriptionRepository: SubscriptionRepository,
    private val billingHistoryEntryRepository: BillingHistoryEntryRepository,
    private val onboardingApplicationRepository: OnboardingApplicationRepository
) {

    @Transactional(readOnly = true)
    fun getDashboard(): AdminDashboardResponse {
        val subscriptions = subscriptionRepository.findAll()
        val billingEntries = billingHistoryEntryRepository.findAll()
        val onboardingApplications = onboardingApplicationRepository.findAll()
        val months = lastMonths(12)
        val currentMonth = YearMonth.now()

        val activeSubscriptions = subscriptions.filter { it.status == SubscriptionStatus.ACTIVE }
        val currentMrr = activeSubscriptions.sumMoney { it.plan.priceBrl }
        val revenueThisMonth = billingEntries
            .filter { it.status == "PAID" && YearMonth.from(it.billedAt) == currentMonth }
            .sumMoney { it.amountBrl }
        val cancellationsThisMonth = subscriptions.count {
            it.status == SubscriptionStatus.CANCELLED && YearMonth.from(it.updatedAt.toLocalDate()) == currentMonth
        }.toLong()
        val newSubscriptionsThisMonth = subscriptions.count {
            YearMonth.from(it.createdAt.toLocalDate()) == currentMonth
        }.toLong()

        val metrics = AdminDashboardMetricsResponse(
            mrrAtual = currentMrr,
            receitaDoMes = revenueThisMonth,
            assinaturasAtivas = activeSubscriptions.size.toLong(),
            churnMensal = churn(cancellationsThisMonth, activeSubscriptions.size.toLong() + cancellationsThisMonth),
            arpa = average(currentMrr, activeSubscriptions.size),
            projecao12m = projectMrr(months, subscriptions, 12),
            arr = currentMrr.multiply(BigDecimal("12")),
            novasAssinaturasNoMes = newSubscriptionsThisMonth,
            cancelamentosNoMes = cancellationsThisMonth,
            cadastrosPendentesDeAprovacao = onboardingApplications.count { it.status.isPendingApproval() }.toLong(),
            ticketMedio = average(
                revenueThisMonth,
                billingEntries.count { it.status == "PAID" && YearMonth.from(it.billedAt) == currentMonth }
            )
        )

        return AdminDashboardResponse(
            metrics = metrics,
            revenueLast12Months = months.map { month ->
                AdminRevenuePointResponse(
                    month = label(month),
                    revenueBrl = billingEntries
                        .filter { it.status == "PAID" && YearMonth.from(it.billedAt) == month }
                        .sumMoney { it.amountBrl }
                )
            },
            subscriberGrowth = months.map { month ->
                AdminSubscriberPointResponse(
                    month = label(month),
                    activeSubscribers = subscriptions.countActiveAt(month).toLong(),
                    projectedMrr = projectMrr(months, subscriptions, months.indexOf(month) + 1)
                )
            },
            newVsCancellations = months.map { month ->
                AdminSubscriptionMovementPointResponse(
                    month = label(month),
                    newSubscriptions = subscriptions.count { YearMonth.from(it.createdAt.toLocalDate()) == month }.toLong(),
                    cancellations = subscriptions.count {
                        it.status == SubscriptionStatus.CANCELLED && YearMonth.from(it.updatedAt.toLocalDate()) == month
                    }.toLong()
                )
            },
            revenueByPlan = activeSubscriptions
                .groupBy { it.plan.displayName() }
                .map { (planName, rows) ->
                    AdminPlanRevenueResponse(
                        planName = planName,
                        mrrBrl = rows.sumMoney { it.plan.priceBrl },
                        subscriptions = rows.size.toLong()
                    )
                }
                .sortedByDescending { it.mrrBrl },
            projections = listOf(3, 6, 12).map { monthsAhead ->
                AdminProjectionResponse(
                    months = monthsAhead,
                    projectedMrrBrl = projectMrr(months, subscriptions, monthsAhead),
                    averageMonthlyGrowthBrl = averageRecentMrrGrowth(months, subscriptions)
                )
            },
            subscriptions = subscriptions
                .sortedByDescending { it.createdAt }
                .map { it.toRow() }
        )
    }

    private fun Subscription.toRow(): AdminSubscriptionRowResponse =
        AdminSubscriptionRowResponse(
            empresa = organization.name,
            plano = plan.displayName(),
            status = status.name,
            mrr = if (status == SubscriptionStatus.ACTIVE) plan.priceBrl else BigDecimal.ZERO,
            proximaCobranca = renewsAt,
            origem = if (organization.referralCode.isNullOrBlank()) "Importado" else "Onboarding",
            inicio = createdAt.toLocalDate(),
            cancelamento = if (status == SubscriptionStatus.CANCELLED) updatedAt.toLocalDate() else null,
            motivoCancelamento = if (status == SubscriptionStatus.CANCELLED) "Cancelamento registrado" else null
        )

    private fun projectMrr(months: List<YearMonth>, subscriptions: List<Subscription>, monthsAhead: Int): BigDecimal {
        val currentMrr = subscriptions
            .filter { it.status == SubscriptionStatus.ACTIVE }
            .sumMoney { it.plan.priceBrl }
        val growth = averageRecentMrrGrowth(months, subscriptions)
        return currentMrr.add(growth.multiply(BigDecimal(monthsAhead))).setScale(2, RoundingMode.HALF_UP)
    }

    private fun averageRecentMrrGrowth(months: List<YearMonth>, subscriptions: List<Subscription>): BigDecimal {
        val recent = months.takeLast(4).map { month -> mrrAt(month, subscriptions) }
        if (recent.size < 2) return BigDecimal.ZERO
        val diffs = recent.zipWithNext { previous, next -> next.subtract(previous) }
        return diffs.sumMoney { it }.divide(BigDecimal(diffs.size), 2, RoundingMode.HALF_UP)
    }

    private fun mrrAt(month: YearMonth, subscriptions: List<Subscription>): BigDecimal =
        subscriptions
            .filter { it.status == SubscriptionStatus.ACTIVE && !YearMonth.from(it.createdAt.toLocalDate()).isAfter(month) }
            .sumMoney { it.plan.priceBrl }

    private fun List<Subscription>.countActiveAt(month: YearMonth): Int =
        count { it.status == SubscriptionStatus.ACTIVE && !YearMonth.from(it.createdAt.toLocalDate()).isAfter(month) }

    private fun churn(cancellations: Long, base: Long): BigDecimal =
        if (base == 0L) BigDecimal.ZERO else BigDecimal(cancellations)
            .multiply(BigDecimal("100"))
            .divide(BigDecimal(base), 2, RoundingMode.HALF_UP)

    private fun average(total: BigDecimal, count: Int): BigDecimal =
        if (count == 0) BigDecimal.ZERO else total.divide(BigDecimal(count), 2, RoundingMode.HALF_UP)

    private fun label(month: YearMonth): String = "%02d/%04d".format(month.monthValue, month.year)

    private fun lastMonths(count: Long): List<YearMonth> {
        val current = YearMonth.now()
        return (count - 1 downTo 0).map { current.minusMonths(it) }
    }

    private fun <T> Iterable<T>.sumMoney(selector: (T) -> BigDecimal): BigDecimal =
        fold(BigDecimal.ZERO) { acc, item -> acc.add(selector(item)) }.setScale(2, RoundingMode.HALF_UP)
}
