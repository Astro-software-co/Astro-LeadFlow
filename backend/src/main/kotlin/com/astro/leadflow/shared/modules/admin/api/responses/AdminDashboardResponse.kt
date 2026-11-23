package com.astro.leadflow.shared.modules.admin.api.responses

data class AdminDashboardResponse(
    val metrics: AdminDashboardMetricsResponse,
    val revenueLast12Months: List<AdminRevenuePointResponse>,
    val subscriberGrowth: List<AdminSubscriberPointResponse>,
    val newVsCancellations: List<AdminSubscriptionMovementPointResponse>,
    val revenueByPlan: List<AdminPlanRevenueResponse>,
    val projections: List<AdminProjectionResponse>,
    val subscriptions: List<AdminSubscriptionRowResponse>
)
