import type { OriginOption, StageOption } from './types';

export const stageOptions: StageOption[] = [
  { value: 'all', label: 'Todos os estagios' },
  { value: 'chatbot', label: 'Falando com chatbot' },
  { value: 'aguardando', label: 'Aguardando atendimento' },
  { value: 'aguardando_documentacao', label: 'Aguardando documentacao' },
  { value: 'negociacao', label: 'Em negociacao' },
  { value: 'concluido', label: 'Concluido' },
];

export const originOptions: OriginOption[] = [
  { value: 'all', label: 'Todas as origens' },
  { value: 'Direto', label: 'Direto' },
  { value: 'WebMotors', label: 'WebMotors' },
  { value: 'Mercado Livre', label: 'Mercado Livre' },
  { value: 'OLX', label: 'OLX' },
  { value: 'Instagram', label: 'Instagram' },
  { value: 'Facebook', label: 'Facebook' },
  { value: 'Mobiauto', label: 'Mobiauto' },
  { value: 'Napista', label: 'Napista' },
];
