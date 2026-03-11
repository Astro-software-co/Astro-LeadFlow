package com.astro.leadflow.shared.modules.iam.api.requests

data class UpdateNotificationsRequest(
    val notifyNewLead: Boolean? = null,
    val notifyLeadWaiting: Boolean? = null,
    val notifySaleCompleted: Boolean? = null,
    val notifyCardMoved: Boolean? = null,
    val notifyViaEmail: Boolean? = null,
    val notifyViaPush: Boolean? = null
)