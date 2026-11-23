import { z } from 'zod';
import { digitsOnly } from './masks';

// ── Step 1 — Responsável (owner data) ───────────────────────────────────────
export const step1Schema = z.object({
  ownerName: z.string().min(2, 'Nome do responsável é obrigatório'),
  ownerEmail: z.string().email('E-mail inválido'),
  ownerCpf: z
    .string()
    .refine((v) => digitsOnly(v).length === 11, 'CPF deve ter 11 dígitos'),
  ownerPhone: z.string().refine((v) => {
    const d = digitsOnly(v);
    return d.length >= 10 && d.length <= 11;
  }, 'Telefone inválido'),
  ownerRole: z.string().optional(),
});

// ── Step 2 — Concessionária (dealership data) ────────────────────────────────
export const PLAN_OPTIONS = [
  { value: 'BASICO_MONTHLY', label: 'Básico — Mensal' },
  { value: 'PROFISSIONAL_MONTHLY', label: 'Profissional — Mensal' },
  { value: 'ENTERPRISE_MONTHLY', label: 'Enterprise — Mensal' },
] as const;

export const step2Schema = z.object({
  companyName: z.string().min(2, 'Nome da empresa é obrigatório'),
  cnpj: z
    .string()
    .refine((v) => digitsOnly(v).length === 14, 'CNPJ deve ter 14 dígitos'),
  planCode: z.string().min(1, 'Selecione um plano'),
  tradeName: z.string().optional(),
  dealershipSegment: z.string().optional(),
  branchCount: z.string().optional(),
  employeeCount: z.string().optional(),
  stockCount: z.string().optional(),
  monthlyLeadsAvg: z.string().optional(),
  marketYears: z.string().optional(),
  website: z.string().optional(),
});

// ── Step 3 — Endereço ────────────────────────────────────────────────────────
export const step3Schema = z.object({
  cep: z
    .string()
    .refine((v) => digitsOnly(v).length === 8, 'CEP deve ter 8 dígitos'),
  street: z.string().min(2, 'Logradouro é obrigatório'),
  number: z.string().optional(),
  complement: z.string().optional(),
  neighborhood: z.string().min(2, 'Bairro é obrigatório'),
  city: z.string().min(2, 'Cidade é obrigatória'),
  state: z.string().min(2, 'Estado é obrigatório'),
});

// ── Step 4 — Integrações (all optional) ─────────────────────────────────────
export const step4Schema = z.object({
  intendedMarketplaces: z
    .array(z.enum(['WEBMOTORS', 'MERCADO_LIVRE', 'OLX']))
    .optional(),
  otherMarketplaces: z.string().max(500).optional(),
});

// ── Step 5 — Revisão ─────────────────────────────────────────────────────────
export const step5Schema = z.object({
  referralCode: z.string().max(32).optional(),
});

// ── Full form schema ──────────────────────────────────────────────────────────
export const onboardingSchema = step1Schema
  .merge(step2Schema)
  .merge(step3Schema)
  .merge(step4Schema)
  .merge(step5Schema);

export type OnboardingFormValues = z.infer<typeof onboardingSchema>;

export const stepSchemas = [
  step1Schema,
  step2Schema,
  step3Schema,
  step4Schema,
  step5Schema,
] as const;

// Fields validated on each step before advancing (empty = all optional, skip trigger)
export const stepFields: Array<(keyof OnboardingFormValues)[]> = [
  ['ownerName', 'ownerEmail', 'ownerCpf', 'ownerPhone'],
  ['companyName', 'cnpj', 'planCode'],
  ['cep', 'street', 'neighborhood', 'city', 'state'],
  [],
  [],
];
