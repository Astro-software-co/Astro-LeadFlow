import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { Loader2 } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Checkbox } from '@/components/ui/checkbox';
import { Badge } from '@/components/ui/badge';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { OnboardingStepper } from '@/features/onboarding/OnboardingStepper';
import { OnboardingSuccess } from '@/features/onboarding/OnboardingSuccess';
import { onboardingSchema, stepFields, PLAN_OPTIONS } from '@/features/onboarding/schema';
import type { OnboardingFormValues } from '@/features/onboarding/schema';
import { maskCNPJ, maskCPF, maskPhone, maskCEP, digitsOnly } from '@/features/onboarding/masks';
import { fetchViaCEP } from '@/features/onboarding/viacep';
import { onboardingService } from '@/services/onboarding.service';
import type { MarketplaceTypeDTO } from '@/types/api/onboarding';

// ── Constants ────────────────────────────────────────────────────────────────

const STEPS = [
  { index: 0, label: 'Responsável' },
  { index: 1, label: 'Concessionária' },
  { index: 2, label: 'Endereço' },
  { index: 3, label: 'Integrações' },
  { index: 4, label: 'Revisão' },
];

const MARKETPLACE_OPTIONS: { value: MarketplaceTypeDTO; label: string }[] = [
  { value: 'WEBMOTORS', label: 'WebMotors' },
  { value: 'MERCADO_LIVRE', label: 'Mercado Livre' },
  { value: 'OLX', label: 'OLX' },
];

// ── Step content components ──────────────────────────────────────────────────

interface StepProps {
  form: ReturnType<typeof useForm<OnboardingFormValues>>;
}

interface Step3Props extends StepProps {
  cepLoading: boolean;
  cepError?: string;
  onCepBlur: () => void;
}

function Step1Responsavel({ form }: StepProps) {
  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">Responsável</h2>

      <FormField
        control={form.control}
        name="ownerName"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Nome do responsável *</FormLabel>
            <FormControl>
              <Input placeholder="Ex: João Silva" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />

      <FormField
        control={form.control}
        name="ownerEmail"
        render={({ field }) => (
          <FormItem>
            <FormLabel>E-mail *</FormLabel>
            <FormControl>
              <Input type="email" placeholder="joao@exemplo.com" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />

      <FormField
        control={form.control}
        name="ownerCpf"
        render={({ field }) => (
          <FormItem>
            <FormLabel>CPF *</FormLabel>
            <FormControl>
              <Input
                placeholder="000.000.000-00"
                value={field.value}
                onChange={(e) => field.onChange(maskCPF(e.target.value))}
                onBlur={field.onBlur}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />

      <FormField
        control={form.control}
        name="ownerPhone"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Telefone *</FormLabel>
            <FormControl>
              <Input
                placeholder="(00) 00000-0000"
                value={field.value}
                onChange={(e) => field.onChange(maskPhone(e.target.value))}
                onBlur={field.onBlur}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />

      <FormField
        control={form.control}
        name="ownerRole"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Cargo</FormLabel>
            <FormControl>
              <Input placeholder="Ex: Diretor Comercial" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </div>
  );
}

function Step2Concessionaria({ form }: StepProps) {
  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">Dados da concessionária</h2>

      <FormField
        control={form.control}
        name="companyName"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Nome da empresa *</FormLabel>
            <FormControl>
              <Input placeholder="Ex: Astro Motors" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />

      <FormField
        control={form.control}
        name="cnpj"
        render={({ field }) => (
          <FormItem>
            <FormLabel>CNPJ *</FormLabel>
            <FormControl>
              <Input
                placeholder="00.000.000/0000-00"
                value={field.value}
                onChange={(e) => field.onChange(maskCNPJ(e.target.value))}
                onBlur={field.onBlur}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />

      <FormField
        control={form.control}
        name="planCode"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Plano desejado *</FormLabel>
            <div className="flex flex-col gap-2 mt-1">
              {PLAN_OPTIONS.map((opt) => (
                <label
                  key={opt.value}
                  className="flex items-center gap-3 cursor-pointer"
                >
                  <input
                    type="radio"
                    name="planCode"
                    value={opt.value}
                    checked={field.value === opt.value}
                    onChange={() => field.onChange(opt.value)}
                    className="accent-primary"
                  />
                  <span className="text-sm">{opt.label}</span>
                </label>
              ))}
            </div>
            <FormMessage />
          </FormItem>
        )}
      />

      <FormField
        control={form.control}
        name="tradeName"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Nome fantasia</FormLabel>
            <FormControl>
              <Input placeholder="Ex: Astro Veículos" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />

      <FormField
        control={form.control}
        name="dealershipSegment"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Segmento</FormLabel>
            <FormControl>
              <Input placeholder="Ex: Multimarcas, Concessionária, Loja" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />

      <div className="grid grid-cols-2 gap-4">
        <FormField
          control={form.control}
          name="branchCount"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Filiais</FormLabel>
              <FormControl>
                <Input type="number" min="1" placeholder="1" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="employeeCount"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Funcionários</FormLabel>
              <FormControl>
                <Input type="number" min="1" placeholder="10" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="stockCount"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Veículos no estoque</FormLabel>
              <FormControl>
                <Input type="number" min="0" placeholder="50" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="monthlyLeadsAvg"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Leads por mês</FormLabel>
              <FormControl>
                <Input type="number" min="0" placeholder="100" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="marketYears"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Anos no mercado</FormLabel>
              <FormControl>
                <Input type="number" min="0" placeholder="5" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>

      <FormField
        control={form.control}
        name="website"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Site</FormLabel>
            <FormControl>
              <Input type="url" placeholder="https://suaempresa.com.br" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </div>
  );
}

function Step3Endereco({ form, cepLoading, cepError, onCepBlur }: Step3Props) {
  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">Endereço</h2>

      <FormField
        control={form.control}
        name="cep"
        render={({ field }) => (
          <FormItem>
            <FormLabel>CEP *</FormLabel>
            <div className="relative">
              <FormControl>
                <Input
                  placeholder="00000-000"
                  value={field.value}
                  onChange={(e) => field.onChange(maskCEP(e.target.value))}
                  onBlur={() => {
                    field.onBlur();
                    onCepBlur();
                  }}
                  className={cepLoading ? 'pr-10' : ''}
                />
              </FormControl>
              {cepLoading && (
                <Loader2 className="absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-muted-foreground" />
              )}
            </div>
            {cepLoading && (
              <p className="text-xs text-muted-foreground">Buscando endereco...</p>
            )}
            {cepError && !cepLoading && (
              <p className="text-xs text-destructive">{cepError}</p>
            )}
            <FormMessage />
          </FormItem>
        )}
      />

      <FormField
        control={form.control}
        name="street"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Logradouro *</FormLabel>
            <FormControl>
              <Input placeholder="Av. Paulista" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />

      <div className="grid grid-cols-2 gap-4">
        <FormField
          control={form.control}
          name="number"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Número</FormLabel>
              <FormControl>
                <Input placeholder="100" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="complement"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Complemento</FormLabel>
              <FormControl>
                <Input placeholder="Sala 1" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>

      <FormField
        control={form.control}
        name="neighborhood"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Bairro *</FormLabel>
            <FormControl>
              <Input placeholder="Bela Vista" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />

      <div className="grid grid-cols-2 gap-4">
        <FormField
          control={form.control}
          name="city"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Cidade *</FormLabel>
              <FormControl>
                <Input placeholder="São Paulo" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="state"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Estado *</FormLabel>
              <FormControl>
                <Input placeholder="SP" maxLength={2} {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>
    </div>
  );
}

function Step4Integracoes({ form }: StepProps) {
  const selected = form.watch('intendedMarketplaces') ?? [];

  function toggleMarketplace(value: MarketplaceTypeDTO) {
    const current = form.getValues('intendedMarketplaces') ?? [];
    form.setValue(
      'intendedMarketplaces',
      current.includes(value) ? current.filter((v) => v !== value) : [...current, value],
      { shouldValidate: true },
    );
  }

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">Integrações</h2>
      <p className="text-sm text-muted-foreground">
        Selecione os marketplaces que você usa para capturar leads.
      </p>

      <div className="space-y-3">
        {MARKETPLACE_OPTIONS.map((opt) => (
          <div key={opt.value} className="flex items-center gap-3">
            <Checkbox
              id={`marketplace-${opt.value}`}
              checked={selected.includes(opt.value)}
              onCheckedChange={() => toggleMarketplace(opt.value)}
            />
            <label
              htmlFor={`marketplace-${opt.value}`}
              className="text-sm font-medium leading-none cursor-pointer"
            >
              {opt.label}
            </label>
          </div>
        ))}
      </div>

      <FormField
        control={form.control}
        name="otherMarketplaces"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Outros canais</FormLabel>
            <FormControl>
              <Textarea
                placeholder="Ex: Site próprio, Instagram, indicações..."
                className="resize-none"
                rows={3}
                {...field}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </div>
  );
}

function Step5Revisao({ form }: StepProps) {
  const values = form.getValues();
  const selected = values.intendedMarketplaces ?? [];
  const marketplaceLabels = MARKETPLACE_OPTIONS.filter((o) => selected.includes(o.value))
    .map((o) => o.label)
    .join(', ');

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">Revisão</h2>
      <p className="text-sm text-muted-foreground">
        Revise as informações antes de enviar o cadastro.
      </p>

      <div className="space-y-3 rounded-lg border bg-muted/20 p-4 text-sm">
        <div>
          <span className="text-xs text-muted-foreground">Responsável</span>
          <p className="font-medium">{values.ownerName}</p>
          <p className="text-muted-foreground">{values.ownerEmail}</p>
          <p className="text-muted-foreground">
            {values.ownerCpf} · {values.ownerPhone}
          </p>
        </div>
        <div>
          <span className="text-xs text-muted-foreground">Empresa</span>
          <p className="font-medium">{values.companyName}</p>
          <p className="text-muted-foreground">{values.cnpj}</p>
        </div>
        <div>
          <span className="text-xs text-muted-foreground">Endereço</span>
          <p className="font-medium">
            {values.street}
            {values.number ? `, ${values.number}` : ''}
          </p>
          <p className="text-muted-foreground">
            {values.neighborhood} · {values.city} – {values.state} · {values.cep}
          </p>
        </div>
        {marketplaceLabels && (
          <div>
            <span className="text-xs text-muted-foreground">Integrações</span>
            <p className="font-medium">{marketplaceLabels}</p>
          </div>
        )}
      </div>

      <FormField
        control={form.control}
        name="referralCode"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Código de indicação</FormLabel>
            <FormControl>
              <Input placeholder="Opcional" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </div>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function Onboarding() {
  const [searchParams] = useSearchParams();
  const refFromUrl = searchParams.get('ref') ?? '';

  const [currentStep, setCurrentStep] = useState(0);
  const [isSuccess, setIsSuccess] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [cepLoading, setCepLoading] = useState(false);
  const [cepError, setCepError] = useState<string | undefined>(undefined);

  const prefersReduced = useReducedMotion();

  const form = useForm<OnboardingFormValues>({
    resolver: zodResolver(onboardingSchema),
    defaultValues: {
      ownerName: '',
      ownerEmail: '',
      ownerCpf: '',
      ownerPhone: '',
      ownerRole: '',
      companyName: '',
      cnpj: '',
      tradeName: '',
      dealershipSegment: '',
      branchCount: '',
      employeeCount: '',
      stockCount: '',
      monthlyLeadsAvg: '',
      marketYears: '',
      website: '',
      cep: '',
      street: '',
      number: '',
      complement: '',
      neighborhood: '',
      city: '',
      state: '',
      intendedMarketplaces: [],
      otherMarketplaces: '',
      referralCode: refFromUrl,
    },
    mode: 'onBlur',
  });

  // ── CEP lookup ──────────────────────────────────────────────────────────────
  async function handleCepBlur() {
    const cepValue = form.getValues('cep');
    const digits = digitsOnly(cepValue);
    if (digits.length !== 8) return;

    setCepLoading(true);
    setCepError(undefined);

    try {
      const data = await fetchViaCEP(digits);
      form.setValue('street', data.logradouro, { shouldValidate: true });
      form.setValue('neighborhood', data.bairro, { shouldValidate: true });
      form.setValue('city', data.localidade, { shouldValidate: true });
      form.setValue('state', data.uf, { shouldValidate: true });
    } catch {
      setCepError('Nao encontramos este CEP. Preencha o endereco manualmente.');
    } finally {
      setCepLoading(false);
    }
  }

  // ── Step navigation ─────────────────────────────────────────────────────────
  async function handleContinue() {
    const fields = stepFields[currentStep];
    if (fields && fields.length > 0) {
      const isValid = await form.trigger(fields);
      if (!isValid) return;
    }
    setCurrentStep((prev) => prev + 1);
  }

  function handleBack() {
    setCurrentStep((prev) => Math.max(0, prev - 1));
  }

  // ── Final submit ────────────────────────────────────────────────────────────
  async function handleSubmit() {
    setIsSubmitting(true);
    setSubmitError(null);

    const values = form.getValues();

    try {
      await onboardingService.submit({
        ownerName: values.ownerName,
        ownerEmail: values.ownerEmail,
        ownerCpf: digitsOnly(values.ownerCpf),
        ownerPhone: digitsOnly(values.ownerPhone),
        ...(values.ownerRole ? { ownerRole: values.ownerRole } : {}),
        companyName: values.companyName,
        cnpj: digitsOnly(values.cnpj),
        planCode: values.planCode,
        ...(values.tradeName ? { tradeName: values.tradeName } : {}),
        ...(values.dealershipSegment ? { dealershipSegment: values.dealershipSegment } : {}),
        ...(values.branchCount ? { branchCount: parseInt(values.branchCount) } : {}),
        ...(values.employeeCount ? { employeeCount: parseInt(values.employeeCount) } : {}),
        ...(values.stockCount ? { stockCount: parseInt(values.stockCount) } : {}),
        ...(values.monthlyLeadsAvg ? { monthlyLeadsAvg: parseInt(values.monthlyLeadsAvg) } : {}),
        ...(values.marketYears ? { marketYears: parseInt(values.marketYears) } : {}),
        ...(values.website ? { website: values.website } : {}),
        cep: digitsOnly(values.cep),
        street: values.street,
        ...(values.number ? { number: values.number } : {}),
        ...(values.complement ? { complement: values.complement } : {}),
        neighborhood: values.neighborhood,
        city: values.city,
        state: values.state,
        ...(values.intendedMarketplaces?.length
          ? { intendedMarketplaces: values.intendedMarketplaces }
          : {}),
        ...(values.otherMarketplaces ? { otherMarketplaces: values.otherMarketplaces } : {}),
        ...(values.referralCode ? { referralCode: values.referralCode } : {}),
      });
      setIsSuccess(true);
    } catch {
      setSubmitError('Não foi possível enviar o cadastro. Verifique sua conexão e tente novamente.');
    } finally {
      setIsSubmitting(false);
    }
  }

  // ── Motion config ───────────────────────────────────────────────────────────
  const motionProps = prefersReduced
    ? { initial: {}, animate: {}, exit: {}, transition: { duration: 0 } }
    : {
        initial: { opacity: 0, x: 24 },
        animate: { opacity: 1, x: 0 },
        exit: { opacity: 0, x: -24 },
        transition: { duration: 0.22, ease: 'easeOut' },
      };

  // ── Success screen ──────────────────────────────────────────────────────────
  if (isSuccess) {
    return (
      <main className="min-h-screen bg-background px-4 py-6 md:py-10">
        <div className="max-w-3xl mx-auto">
          <OnboardingSuccess />
        </div>
      </main>
    );
  }

  const isLastStep = currentStep === STEPS.length - 1;

  return (
    <main className="min-h-screen bg-background px-4 py-6 md:py-10">
      <div className="max-w-3xl mx-auto">
        {/* Logo / brand */}
        <div className="mb-6 text-center">
          <span className="text-xl font-semibold text-primary">Astro LeadFlow</span>
        </div>

        {/* Referral badge */}
        {refFromUrl && (
          <div className="mb-4 flex justify-center">
            <Badge variant="secondary" className="text-xs">
              Indicado por: {refFromUrl}
            </Badge>
          </div>
        )}

        {/* Stepper */}
        <OnboardingStepper steps={STEPS} currentStep={currentStep} />

        {/* Step card */}
        <div className="rounded-xl border bg-card p-4 shadow-sm md:p-6">
          <Form {...form}>
            <form onSubmit={(e) => e.preventDefault()}>
              <AnimatePresence mode="wait">
                <motion.div key={currentStep} {...motionProps}>
                  {currentStep === 0 && <Step1Responsavel form={form} />}
                  {currentStep === 1 && <Step2Concessionaria form={form} />}
                  {currentStep === 2 && (
                    <Step3Endereco
                      form={form}
                      cepLoading={cepLoading}
                      cepError={cepError}
                      onCepBlur={handleCepBlur}
                    />
                  )}
                  {currentStep === 3 && <Step4Integracoes form={form} />}
                  {currentStep === 4 && <Step5Revisao form={form} />}
                </motion.div>
              </AnimatePresence>

              {/* Submit error */}
              {submitError && (
                <p className="mt-4 text-sm text-destructive">{submitError}</p>
              )}

              {/* Navigation */}
              <div className="mt-6 flex items-center justify-between gap-3">
                {currentStep > 0 ? (
                  <Button
                    type="button"
                    variant="outline"
                    onClick={handleBack}
                    disabled={isSubmitting}
                  >
                    Voltar
                  </Button>
                ) : (
                  <span />
                )}

                {isLastStep ? (
                  <Button
                    type="button"
                    onClick={handleSubmit}
                    disabled={isSubmitting}
                    className="min-w-[160px]"
                  >
                    {isSubmitting ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                        Enviando...
                      </>
                    ) : (
                      'Enviar cadastro'
                    )}
                  </Button>
                ) : (
                  <Button
                    type="button"
                    onClick={handleContinue}
                    disabled={isSubmitting}
                  >
                    Continuar cadastro
                  </Button>
                )}
              </div>
            </form>
          </Form>
        </div>
      </div>
    </main>
  );
}
