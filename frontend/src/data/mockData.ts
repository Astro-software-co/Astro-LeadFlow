import { Vehicle, Lead, KanbanCard, KPIData, MonthlySalesData, CategorySalesData, RecentSale } from '@/types';

export const mockVehicles: Vehicle[] = [
  { id: '1', marca: 'Toyota', modelo: 'Corolla', ano: 2024, preco: 145000, status: 'disponivel', categoria: 'Sedan' },
  { id: '2', marca: 'Honda', modelo: 'Civic', ano: 2024, preco: 165000, status: 'disponivel', categoria: 'Sedan' },
  { id: '3', marca: 'Ford', modelo: 'Ranger', ano: 2023, preco: 285000, status: 'reservado', categoria: 'Pickup' },
  { id: '4', marca: 'Chevrolet', modelo: 'Onix', ano: 2024, preco: 85000, status: 'disponivel', categoria: 'Hatch' },
  { id: '5', marca: 'Hyundai', modelo: 'Creta', ano: 2023, preco: 125000, status: 'manutencao', categoria: 'SUV' },
  { id: '6', marca: 'Volkswagen', modelo: 'T-Cross', ano: 2024, preco: 135000, status: 'disponivel', categoria: 'SUV' },
  { id: '7', marca: 'Jeep', modelo: 'Compass', ano: 2024, preco: 185000, status: 'disponivel', categoria: 'SUV' },
  { id: '8', marca: 'Nissan', modelo: 'Kicks', ano: 2023, preco: 115000, status: 'reservado', categoria: 'SUV' },
];

export const mockLeads: Lead[] = [
  { id: '1', nome: 'João Silva', telefone: '(11) 98765-4321', origem: 'OLX', dataCriacao: new Date(), ultimaInteracao: new Date() },
  { id: '2', nome: 'Maria Santos', telefone: '(11) 98765-4322', origem: 'Instagram', dataCriacao: new Date(), ultimaInteracao: new Date() },
  { id: '3', nome: 'Pedro Costa', telefone: '(11) 98765-4323', origem: 'Mercado Livre', dataCriacao: new Date(), ultimaInteracao: new Date() },
  { id: '4', nome: 'Ana Oliveira', telefone: '(11) 98765-4324', origem: 'WebMotors', dataCriacao: new Date(), ultimaInteracao: new Date() },
  { id: '5', nome: 'Lucas Ferreira', telefone: '(11) 98765-4325', origem: 'Facebook', dataCriacao: new Date(), ultimaInteracao: new Date() },
  { id: '6', nome: 'Juliana Lima', telefone: '(11) 98765-4326', origem: 'Napista', dataCriacao: new Date(), ultimaInteracao: new Date() },
  { id: '7', nome: 'Fernanda Rocha', telefone: '(11) 98765-4328', origem: 'Direto', dataCriacao: new Date(), ultimaInteracao: new Date() },
];

export const mockKanbanCards: KanbanCard[] = [
  {
    id: 'card-1',
    leadId: '1',
    lead: mockLeads[0],
    vehicleId: '1',
    vehicle: mockVehicles[0],
    coluna: 'chatbot',
    tags: [{ type: 'vehicle', label: 'Toyota Corolla 2024' }],
    interesse: 'Compra à vista',
  },
  {
    id: 'card-2',
    leadId: '2',
    lead: mockLeads[1],
    vehicleId: '2',
    vehicle: mockVehicles[1],
    coluna: 'chatbot',
    tags: [{ type: 'vehicle', label: 'Honda Civic 2024' }],
    interesse: 'Financiamento',
  },
  {
    id: 'card-3',
    leadId: '3',
    lead: mockLeads[2],
    vehicleId: '3',
    vehicle: mockVehicles[2],
    coluna: 'aguardando',
    tags: [{ type: 'vehicle', label: 'Ford Ranger 2023' }],
    interesse: 'Troca com troco',
    orcamento: 250000,
  },
  {
    id: 'card-4',
    leadId: '4',
    lead: mockLeads[3],
    vehicleId: '4',
    vehicle: mockVehicles[3],
    vendedorId: 'seller-1',
    vendedorNome: 'Carlos M.',
    coluna: 'negociacao',
    tags: [
      { type: 'seller', label: 'Carlos M.' },
      { type: 'vehicle', label: 'Chevrolet Onix 2024' },
      { type: 'status', label: 'Em negociação', variant: 'negotiation' },
    ],
    interesse: 'Financiamento',
  },
  {
    id: 'card-5',
    leadId: '5',
    lead: mockLeads[4],
    vehicleId: '5',
    vehicle: mockVehicles[4],
    vendedorId: 'seller-2',
    vendedorNome: 'Ana P.',
    coluna: 'negociacao',
    tags: [
      { type: 'seller', label: 'Ana P.' },
      { type: 'vehicle', label: 'Hyundai Creta 2023' },
      { type: 'status', label: 'Reservado', variant: 'reserved' },
    ],
    interesse: 'Compra à vista',
  },
  {
    id: 'card-6',
    leadId: '6',
    lead: mockLeads[5],
    vehicleId: '6',
    vehicle: mockVehicles[5],
    vendedorId: 'seller-3',
    vendedorNome: 'Roberto S.',
    coluna: 'negociacao',
    tags: [
      { type: 'seller', label: 'Roberto S.' },
      { type: 'vehicle', label: 'VW T-Cross 2024' },
      { type: 'status', label: 'Aguardando docs', variant: 'waiting' },
    ],
    interesse: 'Financiamento',
  },
  {
    id: 'card-7',
    leadId: '7',
    lead: mockLeads[6],
    vehicleId: '8',
    vehicle: mockVehicles[7],
    vendedorId: 'seller-2',
    vendedorNome: 'Ana P.',
    coluna: 'concluido',
    tags: [
      { type: 'seller', label: 'Ana P.' },
      { type: 'vehicle', label: 'Nissan Kicks 2023' },
      { type: 'status', label: 'Concluído', variant: 'completed' },
    ],
    interesse: 'Compra à vista',
  },
];

export const mockKPIData: KPIData = {
  vendasMes: 42,
  vendasMesTrend: 12.5,
  receitaTotal: 2800000,
  receitaTotalTrend: 18.2,
  carrosEstoque: 156,
  carrosEstoqueTrend: -5.4,
  ticketMedio: 66700,
  ticketMedioTrend: 5.1,
  leadsConvertidos: 17.4,
  leadsConvertidosTrend: 2.3,
  tempoMedioResposta: 2.3,
  tempoMedioRespostaTrend: -15.2,
  totalLeads: 242,
  totalLeadsTrend: 8.7,
};

export const mockMonthlySales: MonthlySalesData[] = [
  { month: 'Jan', vendas: 1800, receita: 1200 },
  { month: 'Fev', vendas: 2100, receita: 1400 },
  { month: 'Mar', vendas: 2300, receita: 1600 },
  { month: 'Abr', vendas: 2500, receita: 1800 },
  { month: 'Mai', vendas: 2800, receita: 2000 },
  { month: 'Jun', vendas: 3100, receita: 2400 },
];

export const mockCategorySales: CategorySalesData[] = [
  { categoria: 'SUV', vendidos: 18, emEstoque: 45 },
  { categoria: 'Sedan', vendidos: 12, emEstoque: 38 },
  { categoria: 'Hatch', vendidos: 8, emEstoque: 32 },
  { categoria: 'Pickup', vendidos: 6, emEstoque: 25 },
  { categoria: 'Outros', vendidos: 4, emEstoque: 16 },
];

export const mockRecentSales: RecentSale[] = [
  { cliente: 'João Silva', veiculo: 'Toyota Corolla 2024', valor: 145000, data: '03/02/2026', vendedor: 'Carlos M.' },
  { cliente: 'Maria Santos', veiculo: 'Honda Civic 2024', valor: 165000, data: '02/02/2026', vendedor: 'Ana P.' },
  { cliente: 'Pedro Costa', veiculo: 'Ford Ranger 2023', valor: 285000, data: '01/02/2026', vendedor: 'Carlos M.' },
  { cliente: 'Ana Oliveira', veiculo: 'Chevrolet Onix 2024', valor: 85000, data: '01/02/2026', vendedor: 'Roberto S.' },
  { cliente: 'Lucas Ferreira', veiculo: 'Hyundai Creta 2023', valor: 125000, data: '31/01/2026', vendedor: 'Ana P.' },
  { cliente: 'Juliana Lima', veiculo: 'VW T-Cross 2024', valor: 135000, data: '30/01/2026', vendedor: 'Roberto S.' },
  { cliente: 'Ricardo Alves', veiculo: 'Jeep Compass 2024', valor: 185000, data: '29/01/2026', vendedor: 'Carlos M.' },
  { cliente: 'Fernanda Rocha', veiculo: 'Nissan Kicks 2023', valor: 115000, data: '28/01/2026', vendedor: 'Ana P.' },
];
