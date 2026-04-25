/**
 * OnboardingPage.test.tsx
 *
 * Tests for the public /onboarding 5-step flow (13-02B):
 *   Step 1: Responsável — owner data (required)
 *   Step 2: Concessionária — dealership data (required: companyName, cnpj)
 *   Step 3: Endereço — address with ViaCEP autofill (required)
 *   Step 4: Integrações — marketplace checkboxes + free text (all optional)
 *   Step 5: Revisão — review summary + editable referral code + submit
 *
 * Preserved behaviors:
 *   - Step validation blocks progression when required fields are missing
 *   - Masks render formatted CNPJ/CPF/phone/CEP display values
 *   - CEP blur autofills street/neighborhood/city/state via ViaCEP
 *   - /onboarding?ref=ASTRO123 shows referred badge + prefills referralCode in review
 *   - Submit sends digits-only payload + all new optional fields
 *   - Success screen shows exact required message
 *   - Framer Motion and reduced-motion behavior preserved
 */
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import React from 'react';

// ── Mock onboarding service ─────────────────────────────────────────────────
vi.mock('@/services/onboarding.service', () => ({
  onboardingService: { submit: vi.fn() },
}));

// ── Mock framer-motion (avoids jsdom animation issues) ──────────────────────
vi.mock('framer-motion', async () => {
  return {
    AnimatePresence: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    motion: {
      div: ({
        children,
        initial: _initial,
        animate: _animate,
        exit: _exit,
        transition: _transition,
        ...rest
      }: React.HTMLAttributes<HTMLDivElement> & {
        children?: React.ReactNode;
        initial?: unknown;
        animate?: unknown;
        exit?: unknown;
        transition?: unknown;
      }) => <div {...rest}>{children}</div>,
    },
    useReducedMotion: () => false,
  };
});

import { onboardingService } from '@/services/onboarding.service';
import Onboarding from '@/pages/Onboarding';

// ── Helpers ─────────────────────────────────────────────────────────────────

function renderOnboarding(search = '') {
  return render(
    <MemoryRouter initialEntries={[`/onboarding${search}`]}>
      <Onboarding />
    </MemoryRouter>,
  );
}

/** Step 1 — Responsável */
function fillStep1(
  name = 'João Silva',
  email = 'joao@astro.com',
  cpf = '123.456.789-09',
  phone = '(11) 99999-9999',
) {
  fireEvent.change(screen.getByLabelText(/nome do responsável/i), { target: { value: name } });
  fireEvent.change(screen.getByLabelText(/e-mail/i), { target: { value: email } });
  fireEvent.change(screen.getByLabelText(/cpf/i), { target: { value: cpf } });
  fireEvent.change(screen.getByLabelText(/telefone/i), { target: { value: phone } });
}

/** Step 2 — Concessionária */
function fillStep2(companyName = 'Astro Motors', cnpj = '11.222.333/0001-81') {
  fireEvent.change(screen.getByLabelText(/nome da empresa/i), { target: { value: companyName } });
  fireEvent.change(screen.getByLabelText(/cnpj/i), { target: { value: cnpj } });
}

/** Step 3 — Endereço */
function fillStep3(
  cep = '01310-100',
  street = 'Av. Paulista',
  neighborhood = 'Bela Vista',
  city = 'São Paulo',
  state = 'SP',
) {
  fireEvent.change(screen.getByLabelText(/cep/i), { target: { value: cep } });
  fireEvent.change(screen.getByLabelText(/logradouro/i), { target: { value: street } });
  fireEvent.change(screen.getByLabelText(/bairro/i), { target: { value: neighborhood } });
  fireEvent.change(screen.getByLabelText(/cidade/i), { target: { value: city } });
  fireEvent.change(screen.getByLabelText(/estado/i), { target: { value: state } });
}

async function advanceStep() {
  const btn = screen.getByRole('button', { name: /continuar cadastro/i });
  await act(async () => {
    fireEvent.click(btn);
  });
}

// ── Tests ────────────────────────────────────────────────────────────────────

describe('OnboardingPage (5-step flow)', () => {
  const mockSubmit = vi.mocked(onboardingService.submit);

  beforeEach(() => {
    mockSubmit.mockResolvedValue({ id: 'app-1', status: 'PENDING' });
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: () =>
          Promise.resolve({
            logradouro: 'Av. Paulista',
            bairro: 'Bela Vista',
            localidade: 'São Paulo',
            uf: 'SP',
            erro: false,
          }),
      }),
    );
  });

  afterEach(() => {
    vi.clearAllMocks();
    vi.unstubAllGlobals();
  });

  // ── 1. Step 1 renders ─────────────────────────────────────────────────────
  it('renders step 1 (Responsável) on initial load', () => {
    renderOnboarding();
    expect(screen.getByRole('heading', { name: /responsável/i })).toBeInTheDocument();
  });

  // ── 2. Step 1 validation blocks progression ──────────────────────────────
  it('blocks progression to step 2 when step 1 fields are empty', async () => {
    renderOnboarding();
    await advanceStep();
    expect(screen.getByRole('heading', { name: /responsável/i })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /concessionária/i })).not.toBeInTheDocument();
  });

  // ── 3. Advance to step 2 when step 1 is valid ────────────────────────────
  it('advances to step 2 (Concessionária) when step 1 is filled', async () => {
    renderOnboarding();
    fillStep1();
    await advanceStep();
    expect(screen.getByRole('heading', { name: /concessionária/i })).toBeInTheDocument();
  });

  // ── 4. CPF mask on step 1 ────────────────────────────────────────────────
  it('renders CPF mask in step 1', () => {
    renderOnboarding();
    const cpfField = screen.getByLabelText(/cpf/i);
    fireEvent.change(cpfField, { target: { value: '12345678909' } });
    expect((cpfField as HTMLInputElement).value).toMatch(/\d{3}\.\d{3}\.\d{3}-\d{2}/);
  });

  // ── 5. Phone mask on step 1 ──────────────────────────────────────────────
  it('renders phone mask in step 1', () => {
    renderOnboarding();
    const phoneField = screen.getByLabelText(/telefone/i);
    fireEvent.change(phoneField, { target: { value: '11999999999' } });
    expect((phoneField as HTMLInputElement).value).toMatch(/\(\d{2}\) \d{4,5}-\d{4}/);
  });

  // ── 6. CNPJ mask on step 2 ───────────────────────────────────────────────
  it('renders CNPJ mask in step 2 after advancing from step 1', async () => {
    renderOnboarding();
    fillStep1();
    await advanceStep();
    const cnpjField = screen.getByLabelText(/cnpj/i);
    fireEvent.change(cnpjField, { target: { value: '11222333000181' } });
    expect((cnpjField as HTMLInputElement).value).toMatch(/\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2}/);
  });

  // ── 7. CEP mask on step 3 ────────────────────────────────────────────────
  it('renders CEP mask in step 3', async () => {
    renderOnboarding();
    fillStep1();
    await advanceStep();
    fillStep2();
    await advanceStep();
    const cepField = screen.getByLabelText(/cep/i);
    fireEvent.change(cepField, { target: { value: '01310100' } });
    expect((cepField as HTMLInputElement).value).toMatch(/\d{5}-\d{3}/);
  });

  // ── 8. ViaCEP autofills address on CEP blur ───────────────────────────────
  it('autofills address fields on CEP blur with 8 digits', async () => {
    renderOnboarding();
    fillStep1();
    await advanceStep();
    fillStep2();
    await advanceStep();

    const cepField = screen.getByLabelText(/cep/i);
    fireEvent.change(cepField, { target: { value: '01310-100' } });
    await act(async () => { fireEvent.blur(cepField); });

    await waitFor(() => {
      expect(screen.getByLabelText(/logradouro/i)).toHaveValue('Av. Paulista');
    });
    expect(screen.getByLabelText(/bairro/i)).toHaveValue('Bela Vista');
    expect(screen.getByLabelText(/cidade/i)).toHaveValue('São Paulo');
    expect(screen.getByLabelText(/estado/i)).toHaveValue('SP');
  });

  // ── 9. ViaCEP loading text ─────────────────────────────────────────────────
  it('shows "Buscando endereco..." while fetching CEP', async () => {
    let resolveFetch!: (value: unknown) => void;
    const pendingFetch = new Promise((resolve) => { resolveFetch = resolve; });
    vi.stubGlobal('fetch', vi.fn().mockReturnValue(pendingFetch));

    renderOnboarding();
    fillStep1();
    await advanceStep();
    fillStep2();
    await advanceStep();

    const cepField = screen.getByLabelText(/cep/i);
    fireEvent.change(cepField, { target: { value: '01310-100' } });
    await act(async () => { fireEvent.blur(cepField); });

    expect(screen.getByText(/buscando endereco/i)).toBeInTheDocument();

    await act(async () => {
      resolveFetch({
        ok: true,
        json: () => Promise.resolve({
          logradouro: 'Av. Paulista',
          bairro: 'Bela Vista',
          localidade: 'São Paulo',
          uf: 'SP',
        }),
      });
    });
  });

  // ── 10. ViaCEP fallback message ───────────────────────────────────────────
  it('shows fallback message when ViaCEP fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('Network error')));

    renderOnboarding();
    fillStep1();
    await advanceStep();
    fillStep2();
    await advanceStep();

    const cepField = screen.getByLabelText(/cep/i);
    fireEvent.change(cepField, { target: { value: '01310-100' } });
    await act(async () => { fireEvent.blur(cepField); });

    await waitFor(() => {
      expect(screen.getByText(/nao encontramos este cep/i)).toBeInTheDocument();
    });
  });

  // ── 11. Step 4 (Integrações) shows marketplace checkboxes ────────────────
  it('shows marketplace checkboxes in step 4 (Integrações)', async () => {
    renderOnboarding();
    fillStep1();
    await advanceStep();
    fillStep2();
    await advanceStep();
    fillStep3();
    await advanceStep(); // → step 4

    expect(screen.getByRole('heading', { name: /integrações/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/webmotors/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/mercado livre/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/olx/i)).toBeInTheDocument();
  });

  // ── 12. Step 5 (Revisão) reachable after completing steps 1–4 ────────────
  it('shows step 5 (Revisão) after completing steps 1–4', async () => {
    renderOnboarding();
    fillStep1();
    await advanceStep();
    fillStep2();
    await advanceStep();
    fillStep3();
    await advanceStep();
    await advanceStep(); // step 4 → step 5

    expect(screen.getByRole('heading', { name: /revisão/i })).toBeInTheDocument();
  });

  // ── 13. Referral badge appears when ?ref= is present ─────────────────────
  it('shows referred badge when ?ref=ASTRO123 is in the URL', () => {
    renderOnboarding('?ref=ASTRO123');
    expect(screen.getByText(/astro123/i)).toBeInTheDocument();
  });

  // ── 14. ?ref= prefills referral code input in review step ────────────────
  it('prefills referral code input in review step from ?ref= query param', async () => {
    renderOnboarding('?ref=ASTRO123');
    fillStep1();
    await advanceStep();
    fillStep2();
    await advanceStep();
    fillStep3();
    await advanceStep();
    await advanceStep(); // → step 5

    const refInput = screen.getByLabelText(/código de indicação/i);
    expect((refInput as HTMLInputElement).value).toBe('ASTRO123');
  });

  // ── 15. Submit includes referralCode from review step ────────────────────
  it('includes referralCode: ASTRO123 when submitting with ?ref=ASTRO123', async () => {
    renderOnboarding('?ref=ASTRO123');

    fillStep1('João Silva', 'joao@astro.com', '123.456.789-09', '(11) 99999-9999');
    await advanceStep();
    fillStep2('Astro Motors', '11.222.333/0001-81');
    await advanceStep();
    fillStep3('01310-100', 'Av. Paulista', 'Bela Vista', 'São Paulo', 'SP');
    await advanceStep();
    await advanceStep(); // → revisão

    const submitBtn = screen.getByRole('button', { name: /enviar cadastro/i });
    await act(async () => { fireEvent.click(submitBtn); });

    await waitFor(() => { expect(mockSubmit).toHaveBeenCalled(); });
    expect(mockSubmit.mock.calls[0][0].referralCode).toBe('ASTRO123');
  });

  // ── 16. Submit payload contains digits-only values ────────────────────────
  it('sends digits-only values for CNPJ, CPF, phone, and CEP', async () => {
    renderOnboarding();

    fillStep1('João Silva', 'joao@astro.com', '123.456.789-09', '(11) 99999-9999');
    await advanceStep();
    fillStep2('Astro Motors', '11.222.333/0001-81');
    await advanceStep();
    fillStep3('01310-100', 'Av. Paulista', 'Bela Vista', 'São Paulo', 'SP');
    await advanceStep();
    await advanceStep(); // → revisão

    const submitBtn = screen.getByRole('button', { name: /enviar cadastro/i });
    await act(async () => { fireEvent.click(submitBtn); });

    await waitFor(() => { expect(mockSubmit).toHaveBeenCalled(); });

    const callArg = mockSubmit.mock.calls[0][0];
    expect(callArg.cnpj).toMatch(/^\d+$/);
    expect(callArg.ownerCpf).toMatch(/^\d+$/);
    expect(callArg.ownerPhone).toMatch(/^\d+$/);
    expect(callArg.cep).toMatch(/^\d+$/);
  });

  // ── 17. Success message after submit ──────────────────────────────────────
  it('shows exact success message after successful submit', async () => {
    renderOnboarding();

    fillStep1('João Silva', 'joao@astro.com', '123.456.789-09', '(11) 99999-9999');
    await advanceStep();
    fillStep2('Astro Motors', '11.222.333/0001-81');
    await advanceStep();
    fillStep3('01310-100', 'Av. Paulista', 'Bela Vista', 'São Paulo', 'SP');
    await advanceStep();
    await advanceStep(); // → revisão

    const submitBtn = screen.getByRole('button', { name: /enviar cadastro/i });
    await act(async () => { fireEvent.click(submitBtn); });

    await waitFor(() => {
      expect(
        screen.getByText(/Sua solicitação foi enviada e está em análise\. Entraremos em contato em breve\./i),
      ).toBeInTheDocument();
    });
  });

  // ── 18. Route is accessible without auth ─────────────────────────────────
  it('renders without AuthGuard (public route — component mounts successfully)', () => {
    renderOnboarding();
    expect(screen.getByRole('heading', { name: /responsável/i })).toBeInTheDocument();
  });
});
