import { z } from 'zod';
import { digitsOnly } from './masks';

// ── Step 1 — Dados da empresa ───────────────────────────────────────────────
export const step1Schema = z.object({
  companyName: z.string().min(2, 'Nome da empresa é obrigatório'),
  cnpj: z
    .string()
    .refine((v) => digitsOnly(v).length === 14, 'CNPJ deve ter 14 dígitos'),
});

// ── Step 2 — Responsável ────────────────────────────────────────────────────
export const step2Schema = z.object({
  ownerName: z.string().min(2, 'Nome do responsável é obrigatório'),
  ownerEmail: z.string().email('E-mail inválido'),
  ownerCpf: z
    .string()
    .refine((v) => digitsOnly(v).length === 11, 'CPF deve ter 11 dígitos'),
  ownerPhone: z
    .string()
    .refine((v) => {
      const d = digitsOnly(v);
      return d.length >= 10 && d.length <= 11;
    }, 'Telefone inválido'),
});

// ── Step 3 — Endereço ────────────────────────────────────────────────────────
export const step3Schema = z.object({
  cep: z
    .string()
    .refine((v) => digitsOnly(v).length === 8, 'CEP deve ter 8 dígitos'),
  street: z.string().min(2, 'Logradouro é obrigatório'),
  neighborhood: z.string().min(2, 'Bairro é obrigatório'),
  city: z.string().min(2, 'Cidade é obrigatória'),
  state: z.string().min(2, 'Estado é obrigatório'),
});

// ── Full form schema (union of all steps) ───────────────────────────────────
export const onboardingSchema = step1Schema.merge(step2Schema).merge(step3Schema);

export type OnboardingFormValues = z.infer<typeof onboardingSchema>;

// Per-step schemas used for trigger validation before advancing
export const stepSchemas = [step1Schema, step2Schema, step3Schema] as const;

// Fields that belong to each step (for trigger-based validation)
export const stepFields: Array<(keyof OnboardingFormValues)[]> = [
  ['companyName', 'cnpj'],
  ['ownerName', 'ownerEmail', 'ownerCpf', 'ownerPhone'],
  ['cep', 'street', 'neighborhood', 'city', 'state'],
];
