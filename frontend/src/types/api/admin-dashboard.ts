export interface AdminDashboardMetricsDTO {
  mrrAtual: number;
  receitaDoMes: number;
  assinaturasAtivas: number;
  churnMensal: number;
  arpa: number;
  projecao12m: number;
  arr: number;
  novasAssinaturasNoMes: number;
  cancelamentosNoMes: number;
  cadastrosPendentesDeAprovacao: number;
  ticketMedio: number;
}

export interface AdminRevenuePointDTO {
  month: string;
  revenueBrl: number;
}

export interface AdminSubscriberPointDTO {
  month: string;
  activeSubscribers: number;
  projectedMrr: number;
}

export interface AdminSubscriptionMovementPointDTO {
  month: string;
  newSubscriptions: number;
  cancellations: number;
}

export interface AdminPlanRevenueDTO {
  planName: string;
  mrrBrl: number;
  subscriptions: number;
}

export interface AdminProjectionDTO {
  months: number;
  projectedMrrBrl: number;
  averageMonthlyGrowthBrl: number;
}

export interface AdminSubscriptionRowDTO {
  empresa: string;
  plano: string;
  status: string;
  mrr: number;
  proximaCobranca: string | null;
  origem: string;
  inicio: string;
  cancelamento: string | null;
  motivoCancelamento: string | null;
}

export interface AdminDashboardDTO {
  metrics: AdminDashboardMetricsDTO;
  revenueLast12Months: AdminRevenuePointDTO[];
  subscriberGrowth: AdminSubscriberPointDTO[];
  newVsCancellations: AdminSubscriptionMovementPointDTO[];
  revenueByPlan: AdminPlanRevenueDTO[];
  projections: AdminProjectionDTO[];
  subscriptions: AdminSubscriptionRowDTO[];
}
