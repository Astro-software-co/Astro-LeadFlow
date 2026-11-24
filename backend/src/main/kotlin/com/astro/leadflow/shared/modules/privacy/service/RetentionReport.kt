package com.astro.leadflow.shared.modules.privacy.service

data class RetentionReport(val eventPayloadsCleared: Int, val inboxPayloadsCleared: Int, val errorsDeleted: Int)
