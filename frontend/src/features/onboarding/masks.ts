/**
 * Input mask helpers.
 *
 * Each function takes the raw input value (may include non-digits already)
 * and returns a formatted display string.
 *
 * The submit payload must strip all non-digits before sending to the backend.
 */

export function digitsOnly(value: string): string {
  return value.replace(/\D/g, '');
}

export function maskCNPJ(value: string): string {
  const digits = digitsOnly(value).slice(0, 14);
  // 00.000.000/0000-00
  return digits
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2}\.\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{2}\.\d{3}\.\d{3})(\d)/, '$1/$2')
    .replace(/^(\d{2}\.\d{3}\.\d{3}\/\d{4})(\d)/, '$1-$2');
}

export function maskCPF(value: string): string {
  const digits = digitsOnly(value).slice(0, 11);
  // 000.000.000-00
  return digits
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3}\.\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3}\.\d{3}\.\d{3})(\d)/, '$1-$2');
}

export function maskPhone(value: string): string {
  const digits = digitsOnly(value).slice(0, 11);
  // (00) 0000-0000 or (00) 00000-0000
  if (digits.length <= 10) {
    return digits
      .replace(/^(\d{2})(\d)/, '($1) $2')
      .replace(/^(\(\d{2}\) \d{4})(\d)/, '$1-$2');
  }
  return digits
    .replace(/^(\d{2})(\d)/, '($1) $2')
    .replace(/^(\(\d{2}\) \d{5})(\d)/, '$1-$2');
}

export function maskCEP(value: string): string {
  const digits = digitsOnly(value).slice(0, 8);
  // 00000-000
  return digits.replace(/^(\d{5})(\d)/, '$1-$2');
}
