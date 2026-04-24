export interface ViaCEPResponse {
  logradouro: string;
  bairro: string;
  localidade: string;
  uf: string;
  erro?: boolean | string;
}

/**
 * Fetches address data from the ViaCEP public API.
 * @param cep digits-only CEP string (8 digits)
 */
export async function fetchViaCEP(cep: string): Promise<ViaCEPResponse> {
  const digits = cep.replace(/\D/g, '');
  if (digits.length !== 8) {
    throw new Error('CEP deve ter 8 digitos');
  }

  const response = await fetch(`https://viacep.com.br/ws/${digits}/json/`);

  if (!response.ok) {
    throw new Error('Falha ao consultar o CEP');
  }

  const data: ViaCEPResponse = await response.json();

  if (data.erro) {
    throw new Error('CEP nao encontrado');
  }

  return data;
}
