package com.astro.leadflow.shared.modules.admin.api.responses

import java.math.BigDecimal

data class AdminDashboardMetricsResponse(
    val mrrAtual: BigDecimal,
    val receitaDoMes: BigDecimal,
    val assinaturasAtivas: Long,
    val churnMensal: BigDecimal,
    val arpa: BigDecimal,
    val projecao12m: BigDecimal,
    val arr: BigDecimal,
    val novasAssinaturasNoMes: Long,
    val cancelamentosNoMes: Long,
    val cadastrosPendentesDeAprovacao: Long,
    val ticketMedio: BigDecimal
)
