package com.astro.leadflow.shared.modules.iam.domain

object RbacPermissions {
    const val FINANCE_VIEW = "finance:view"
    const val ORGANIZATIONS_VIEW = "organizations:view"
    const val ORGANIZATIONS_APPROVE = "organizations:approve"
    const val USERS_MANAGE_ALL = "users:manage_all"
    const val TEAM_VIEW = "team:view"
    const val TEAM_CREATE_SELLER = "team:create_seller"
    const val TEAM_UPDATE_SELLER = "team:update_seller"
    const val TEAM_DELETE_SELLER = "team:delete_seller"
    const val BILLING_VIEW = "billing:view"
    const val PLANS_VIEW = "plans:view"
    const val PLANS_CHANGE = "plans:change"
    const val SETTINGS_SECURITY_UPDATE = "settings:security_update"
    const val LEADS_VIEW = "leads:view"
    const val LEADS_UPDATE = "leads:update"
    const val STOCK_VIEW = "stock:view"

    val ROLE_PERMISSIONS: Map<Role, Set<String>> = mapOf(
        Role.ASTRO_ADMIN to setOf(
            FINANCE_VIEW,
            ORGANIZATIONS_VIEW,
            ORGANIZATIONS_APPROVE,
            USERS_MANAGE_ALL,
        ),
        Role.MANAGER to setOf(
            TEAM_VIEW,
            TEAM_CREATE_SELLER,
            TEAM_UPDATE_SELLER,
            TEAM_DELETE_SELLER,
            BILLING_VIEW,
            PLANS_VIEW,
            PLANS_CHANGE,
            SETTINGS_SECURITY_UPDATE,
            LEADS_VIEW,
            LEADS_UPDATE,
            STOCK_VIEW,
        ),
        Role.SELLER to setOf(
            SETTINGS_SECURITY_UPDATE,
            LEADS_VIEW,
            LEADS_UPDATE,
            STOCK_VIEW,
        ),
    )

    fun hasPermission(role: Role, permission: String): Boolean =
        ROLE_PERMISSIONS[role]?.contains(permission) == true
}
