## ADDED Requirements

### Requirement: E-mail em producao
No perfil `prod`, o sistema SHALL enviar e-mails por um provedor real e SHALL NOT depender do MailHog.

#### Scenario: Convite de usuario
- **WHEN** o `MANAGER` convida um vendedor em producao
- **THEN** o e-mail de convite e entregue pelo provedor configurado
