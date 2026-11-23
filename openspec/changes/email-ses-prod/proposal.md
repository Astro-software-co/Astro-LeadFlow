## Why
O `EmailService` so tem implementacao MailHog (desenvolvimento). Em producao convites de usuario, onboarding e alertas de cota nao enviam e-mail.

## What Changes
- Implementacao do `EmailService` com Amazon SES (SMTP ou SDK), ativada no perfil `prod`.
- Dominio e remetente verificados no SES.

## Impact
Modulo `communication/email`, configuracao do perfil `prod`. Depende da conta AWS.

## Perguntas em aberto
- Qual dominio e remetente oficial?
