export type VehicleStatus = 'disponivel' | 'reservado' | 'manutencao' | 'vendido' | 'aguardando_documentacao';
export type VehicleCategory = 'SUV' | 'Sedan' | 'Hatch' | 'Pickup' | 'Outros';

export interface Vehicle {
  id: string;
  marca: string;
  modelo: string;
  ano: number;
  preco: number;
  precoCusto?: number;
  status: VehicleStatus;
  categoria: VehicleCategory;
  cor?: string;
  placa?: string;
  quilometragem?: number;
  ipvaPago?: boolean;
  fotos?: string[];
}

export type LeadOrigin = 'OLX' | 'Mercado Livre' | 'Instagram' | 'WebMotors' | 'Facebook' | 'Mobiauto' | 'Napista' | 'Direto';

export interface Lead {
  id: string;
  nome: string;
  telefone: string;
  email?: string;
  origem: LeadOrigin;
  dataCriacao: Date;
  ultimaInteracao: Date;
  interesse?: string;
  orcamento?: number;
  urgencia?: 'baixa' | 'media' | 'alta';
  tempoCompra?: string;
  observacoes?: string;
  endereco?: string;
  profissao?: string;
}

export type KanbanColumn = 'chatbot' | 'aguardando' | 'aguardando_documentacao' | 'negociacao' | 'concluido';

export type CardTag = 
  | { type: 'vehicle'; label: string }
  | { type: 'seller'; label: string }
  | { type: 'status'; label: string; variant: 'negotiation' | 'reserved' | 'completed' | 'waiting' };

export interface KanbanCard {
  id: string;
  leadId: string;
  lead: Lead;
  vehicleId?: string;
  vehicle?: Vehicle;
  vendedorId?: string;
  vendedorNome?: string;
  coluna: KanbanColumn;
  tags: CardTag[];
  interesse?: string;
  orcamento?: number;
}

export interface Seller {
  id: string;
  nome: string;
  email: string;
  role: 'ADMIN' | 'VENDEDOR';
}

export interface KPIData {
  vendasMes: number;
  vendasMesTrend: number;
  receitaTotal: number;
  receitaTotalTrend: number;
  carrosEstoque: number;
  carrosEstoqueTrend: number;
  ticketMedio: number;
  ticketMedioTrend: number;
  leadsConvertidos: number;
  leadsConvertidosTrend: number;
  tempoMedioResposta: number;
  tempoMedioRespostaTrend: number;
  totalLeads: number;
  totalLeadsTrend: number;
}

export interface MonthlySalesData {
  month: string;
  vendas: number;
  receita: number;
}

export interface CategorySalesData {
  categoria: string;
  vendidos: number;
  emEstoque: number;
}

export interface RecentSale {
  cliente: string;
  veiculo: string;
  valor: number;
  data: string;
  vendedor: string;
}

export interface Notification {
  id: string;
  title: string;
  message: string;
  type: 'info' | 'warning' | 'success' | 'error';
  read: boolean;
  createdAt: Date;
}

export type ThemeMode = 'light' | 'dark' | 'system';
export type AccentColor = 'purple' | 'blue' | 'green' | 'orange';
