/**
 * OnboardingPage.test.tsx
 *
 * Tests for the public /onboarding flow:
 * - Step-by-step validation blocks progression when required fields are missing
 * - Masks render formatted CNPJ/CPF/phone/CEP display values
 * - CEP blur autofills street/neighborhood/city/state via ViaCEP
 * - /onboarding?ref=ASTRO123 shows referred badge and passes referralCode to service
 * - Submit sends digits-only payload
 * - Success screen shows exact required message
 */
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import React from 'react';

// ── Mock onboarding service ─────────────────────────────────────────────────
// vi.mock is hoisted before variable declarations, so we must NOT reference
// external variables inside the factory. Use vi.fn() directly in the factory.
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

// Import after mocks are set up
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

function fillStep1(companyName = 'Astro Motors', cnpj = '11.222.333/0001-81') {
  fireEvent.change(screen.getByLabelText(/nome da empresa/i), { target: { value: companyName } });
  fireEvent.change(screen.getByLabelText(/cnpj/i), { target: { value: cnpj } });
}

function fillStep2(
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

describe('OnboardingPage', () => {
  const mockSubmit = vi.mocked(onboardingService.submit);

  beforeEach(() => {
    mockSubmit.mockResolvedValue({ id: 'app-1', status: 'PENDING' });
    // Default: ViaCEP succeeds
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
  it('renders step 1 (Dados da empresa) on initial load', () => {
    renderOnboarding();
    // Use getByRole('heading') to uniquely target the step h2 heading
    expect(screen.getByRole('heading', { name: /dados da empresa/i })).toBeInTheDocument();
  });

  // ── 2. Step validation blocks progression ────────────────────────────────
  it('blocks progression to step 2 when step 1 fields are empty', async () => {
    renderOnboarding();
    await advanceStep();
    // Still on step 1 — heading is still present
    expect(screen.getByRole('heading', { name: /dados da empresa/i })).toBeInTheDocument();
    // Should not see step 2 heading
    expect(screen.queryByRole('heading', { name: /^responsável$/i })).not.toBeInTheDocument();
  });

  // ── 3. Advance to step 2 when step 1 is valid ────────────────────────────
  it('advances to step 2 when step 1 fields are filled', async () => {
    renderOnboarding();
    fillStep1();
    await advanceStep();
    expect(screen.getByRole('heading', { name: /responsável/i })).toBeInTheDocument();
  });

  // ── 4. CNPJ mask renders formatted value ─────────────────────────────────
  it('renders CNPJ mask pattern in the CNPJ field', () => {
    renderOnboarding();
    const cnpjField = screen.getByLabelText(/cnpj/i);
    fireEvent.change(cnpjField, { target: { value: '11222333000181' } });
    const displayValue = (cnpjField as HTMLInputElement).value;
    expect(displayValue).toMatch(/\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2}/);
  });

  // ── 5. CPF mask renders formatted value ──────────────────────────────────
  it('renders CPF mask after advancing to step 2', async () => {
    renderOnboarding();
    fillStep1();
    await advanceStep();
    const cpfField = screen.getByLabelText(/cpf/i);
    fireEvent.change(cpfField, { target: { value: '12345678909' } });
    const displayValue = (cpfField as HTMLInputElement).value;
    expect(displayValue).toMatch(/\d{3}\.\d{3}\.\d{3}-\d{2}/);
  });

  // ── 6. Phone mask renders formatted value ────────────────────────────────
  it('renders phone mask after advancing to step 2', async () => {
    renderOnboarding();
    fillStep1();
    await advanceStep();
    const phoneField = screen.getByLabelText(/telefone/i);
    fireEvent.change(phoneField, { target: { value: '11999999999' } });
    const displayValue = (phoneField as HTMLInputElement).value;
    expect(displayValue).toMatch(/\(\d{2}\) \d{4,5}-\d{4}/);
  });

  // ── 7. CEP mask renders formatted value ──────────────────────────────────
  it('renders CEP mask in step 3', async () => {
    renderOnboarding();
    fillStep1();
    await advanceStep();
    fillStep2();
    await advanceStep();
    const cepField = screen.getByLabelText(/cep/i);
    fireEvent.change(cepField, { target: { value: '01310100' } });
    const displayValue = (cepField as HTMLInputElement).value;
    expect(displayValue).toMatch(/\d{5}-\d{3}/);
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
    await act(async () => {
      fireEvent.blur(cepField);
    });

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
    const pendingFetch = new Promise((resolve) => {
      resolveFetch = resolve;
    });
    vi.stubGlobal('fetch', vi.fn().mockReturnValue(pendingFetch));

    renderOnboarding();
    fillStep1();
    await advanceStep();
    fillStep2();
    await advanceStep();

    const cepField = screen.getByLabelText(/cep/i);
    fireEvent.change(cepField, { target: { value: '01310-100' } });
    await act(async () => {
      fireEvent.blur(cepField);
    });

    expect(screen.getByText(/buscando endereco/i)).toBeInTheDocument();

    // Resolve the pending fetch to avoid dangling promises
    await act(async () => {
      resolveFetch({
        ok: true,
        json: () =>
          Promise.resolve({
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
    await act(async () => {
      fireEvent.blur(cepField);
    });

    await waitFor(() => {
      expect(screen.getByText(/nao encontramos este cep/i)).toBeInTheDocument();
    });
  });

  // ── 11. Referral badge appears when ?ref= is present ─────────────────────
  it('shows referred badge when ?ref=ASTRO123 is in the URL', () => {
    renderOnboarding('?ref=ASTRO123');
    expect(screen.getByText(/astro123/i)).toBeInTheDocument();
  });

  // ── 12. Submit includes referralCode from ?ref= ───────────────────────────
  it('includes referralCode: ASTRO123 when submitting with ?ref=ASTRO123', async () => {
    renderOnboarding('?ref=ASTRO123');

    fillStep1('Astro Motors', '11.222.333/0001-81');
    await advanceStep();
    fillStep2('João Silva', 'joao@astro.com', '123.456.789-09', '(11) 99999-9999');
    await advanceStep();
    fillStep3('01310-100', 'Av. Paulista', 'Bela Vista', 'São Paulo', 'SP');
    await advanceStep(); // advance to step 4 (Revisao)

    const submitBtn = screen.getByRole('button', { name: /enviar cadastro/i });
    await act(async () => {
      fireEvent.click(submitBtn);
    });

    await waitFor(() => {
      expect(mockSubmit).toHaveBeenCalled();
    });

    const callArg = mockSubmit.mock.calls[0][0];
    expect(callArg.referralCode).toBe('ASTRO123');
  });

  // ── 13. Submit payload contains digits-only values ────────────────────────
  it('sends digits-only values for CNPJ, CPF, phone, and CEP', async () => {
    renderOnboarding();

    fillStep1('Astro Motors', '11.222.333/0001-81');
    await advanceStep();
    fillStep2('João Silva', 'joao@astro.com', '123.456.789-09', '(11) 99999-9999');
    await advanceStep();
    fillStep3('01310-100', 'Av. Paulista', 'Bela Vista', 'São Paulo', 'SP');
    await advanceStep(); // advance to Revisao

    const submitBtn = screen.getByRole('button', { name: /enviar cadastro/i });
    await act(async () => {
      fireEvent.click(submitBtn);
    });

    await waitFor(() => {
      expect(mockSubmit).toHaveBeenCalled();
    });

    const callArg = mockSubmit.mock.calls[0][0];
    expect(callArg.cnpj).toMatch(/^\d+$/);
    expect(callArg.ownerCpf).toMatch(/^\d+$/);
    expect(callArg.ownerPhone).toMatch(/^\d+$/);
    expect(callArg.cep).toMatch(/^\d+$/);
  });

  // ── 14. Success message after submit ──────────────────────────────────────
  it('shows exact success message after successful submit', async () => {
    renderOnboarding();

    fillStep1('Astro Motors', '11.222.333/0001-81');
    await advanceStep();
    fillStep2('João Silva', 'joao@astro.com', '123.456.789-09', '(11) 99999-9999');
    await advanceStep();
    fillStep3('01310-100', 'Av. Paulista', 'Bela Vista', 'São Paulo', 'SP');
    await advanceStep(); // advance to Revisao

    const submitBtn = screen.getByRole('button', { name: /enviar cadastro/i });
    await act(async () => {
      fireEvent.click(submitBtn);
    });

    await waitFor(() => {
      expect(
        screen.getByText(/Sua solicitação foi enviada e está em análise\. Entraremos em contato em breve\./i),
      ).toBeInTheDocument();
    });
  });

  // ── 15. Step 4 (Revisao) shows summary before submit ─────────────────────
  it('shows step 4 title Revisão after completing steps 1-3', async () => {
    renderOnboarding();
    fillStep1();
    await advanceStep();
    fillStep2();
    await advanceStep();
    fillStep3();
    await advanceStep();
    expect(screen.getByRole('heading', { name: /revisão/i })).toBeInTheDocument();
  });

  // ── 16. Route is accessible without auth (component renders) ─────────────
  it('renders without AuthGuard (public route — component mounts successfully)', () => {
    renderOnboarding();
    expect(screen.getByRole('heading', { name: /dados da empresa/i })).toBeInTheDocument();
  });
});
