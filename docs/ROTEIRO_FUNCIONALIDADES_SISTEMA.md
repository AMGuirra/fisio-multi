# Roteiro Completo de Funcionalidades — FisioGestão

Documento de apoio para apresentação, treinamento e repasse do escopo funcional do sistema de gestão para clínicas de fisioterapia.

---

## 1. Roteiro de apresentação

### 1.1 Abertura

"O FisioGestão foi estruturado para concentrar toda a gestão operacional da clínica em um único ambiente. Ele cobre o ciclo completo de cadastro de pacientes, agendamento, atendimento, prontuário, financeiro e relatórios."

"O acesso é protegido por login e cada usuário opera dentro do seu perfil. Os cadastros administrativos ficam restritos ao perfil ADMIN, enquanto os demais usuários acessam apenas o que lhes foi liberado."

### 1.2 Acesso e segurança

"Na entrada do sistema temos autenticação por usuário e senha. Também existe fluxo de 'esqueci minha senha', redefinição por token, alteração da própria senha e sugestão de senha forte."

"Depois do login, o usuário pode trocar o tema visual (claro/escuro) e navegar pelo menu conforme as permissões do seu perfil."

### 1.3 Painel principal (Dashboard)

"Ao entrar no sistema, o usuário chega ao painel. Ele entrega uma visão executiva com: pacientes ativos, sessões do dia, solicitações pendentes, receitas pendentes, despesas pendentes e saldo projetado."

### 1.4 Pacientes

"Na área de pacientes, é possível cadastrar, editar, inativar e reativar pacientes. O cadastro contempla dados pessoais (nome, telefone, e-mail, CPF, data de nascimento) e endereço completo. O sistema valida CPF duplicado automaticamente."

### 1.5 Agenda

"A agenda é o coração operacional do sistema. O profissional pode visualizar o calendário diário, agendar sessões avulsas ou recorrentes, remarcar, cancelar e marcar como realizada."

"O sistema verifica automaticamente conflitos de horário, respeita o intervalo de almoço e os dias de funcionamento configurados."

"Também há o módulo de bloqueios de agenda, para reservar períodos sem atendimento (férias, reuniões, feriados)."

### 1.6 Solicitações de agendamento online

"Pacientes podem solicitar agendamento pelo portal público sem precisar de login. O profissional recebe a solicitação, analisa e aprova ou recusa. O paciente recebe notificação por e-mail em cada etapa."

### 1.7 Portal do Paciente

"O paciente possui acesso a um portal próprio (via token seguro) onde pode confirmar ou cancelar sessões agendadas e consultar seu histórico."

### 1.8 Planos de tratamento (Pacotes)

"O sistema suporta planos de tratamento com número de sessões predefinido e valor total. As sessões são consumidas conforme o agendamento, com controle de saldo de sessões e estorno automático em cancelamentos."

### 1.9 Prontuário SOAP

"Cada sessão realizada pode ter um registro de prontuário no formato SOAP (Subjetivo, Objetivo, Avaliação, Plano), garantindo o histórico clínico do paciente."

### 1.10 Financeiro

"O módulo financeiro registra lançamentos de receita e despesa. Lançamentos podem ser criados manualmente ou gerados automaticamente a partir de sessões avulsas e planos de tratamento. O profissional faz a baixa quando recebe o pagamento."

"O dashboard financeiro exibe receitas recebidas, receitas pendentes, despesas pagas e despesas pendentes, com taxa de recebimento e ticket médio."

### 1.11 Relatórios

"O sistema gera relatórios operacionais (sessões, faturamento) e documentos exportáveis em PDF e Excel. Também é possível enviar lembretes e cobranças por e-mail diretamente pelo sistema."

### 1.12 Configurações

"As configurações da clínica definem nome, dias e horários de funcionamento, intervalo de almoço, duração padrão das sessões e fuso horário. Também é possível configurar o servidor de e-mail (SMTP) para envio das notificações."

### 1.13 Auditoria

"Toda ação relevante no sistema é registrada no log de auditoria: criações, edições, cancelamentos, exclusões, logins e logouts. O ADMIN pode consultar o histórico completo com detalhes de antes/depois."

---

## 2. Inventário completo de funcionalidades

### 2.1 Acesso e conta

- Login com autenticação obrigatória.
- Logout do sistema.
- Recuperação de senha por e-mail.
- Redefinição de senha com token.
- Alteração da própria senha.
- Troca de tema visual (claro/escuro) pelo usuário.
- Controle de permissão por perfil (ADMIN, PROFISSIONAL, RECEPCIONISTA, VISUALIZADOR).

### 2.2 Painel principal

- Contagem de pacientes ativos (query direta no banco, sem carregar listas).
- Sessões do dia corrente.
- Solicitações pendentes de análise.
- Receitas e despesas pendentes.
- Saldo financeiro projetado.

### 2.3 Pacientes

- Cadastro com dados pessoais e endereço.
- Validação de CPF único.
- Edição e inativação de pacientes.
- Busca por nome, CPF, telefone ou e-mail.
- Portal do paciente com token de acesso seguro.

### 2.4 Agenda

- Calendário diário e semanal.
- Agendamento avulso manual.
- Agendamento recorrente (diário, semanal, quinzenal, mensal).
- Edição e reagendamento de sessões.
- Cancelamento com motivo.
- Marcação como realizada.
- Reversão de realização.
- Reabertura de sessão cancelada.
- Exclusão física de sessão.
- Geração automática de slots disponíveis respeitando horários e bloqueios.
- Bloqueios de agenda por período ou recorrência.
- Validação de conflito de horário em tempo real.

### 2.5 Solicitações de agendamento online

- Formulário público de solicitação (sem login).
- Aprovação, reagendamento ou recusa pelo profissional.
- Notificações automáticas por e-mail ao paciente.

### 2.6 Portal do Paciente

- Acesso via token seguro enviado por e-mail.
- Visualização de próximas sessões.
- Confirmação e cancelamento de sessão pelo próprio paciente.
- Histórico das últimas sessões.

### 2.7 Planos de tratamento

- Cadastro de plano com número de sessões e valor total.
- Consumo automático de sessões no agendamento.
- Estorno automático em cancelamentos.
- Geração automática de lançamento financeiro na venda do plano.

### 2.8 Prontuário SOAP

- Registro por sessão realizada.
- Campos: Subjetivo, Objetivo, Avaliação, Plano.
- Campos específicos de fisioterapia: anamnese, diagnóstico, hipótese diagnóstica, conduta.
- Histórico de prontuários por paciente.

### 2.9 Financeiro

- Lançamentos manuais de receita e despesa.
- Geração automática de receita por sessão avulsa realizada.
- Geração automática de receita por plano de tratamento vendido.
- Baixa manual de lançamento (marcar como pago).
- Cancelamento de baixa.
- Cancelamento de lançamento.
- Edição de lançamento pendente.
- Filtro por período.
- Resumo com receitas recebidas, pendentes, despesas pagas e pendentes.
- Taxa de recebimento e ticket médio calculados no banco de dados (sem carregamento em memória).

### 2.10 Relatórios e exportações

- Relatório de sessões por período.
- Relatório financeiro.
- Exportação em PDF (via OpenPDF).
- Exportação em Excel (via Apache POI).
- Envio de lembrete de sessão por e-mail.
- Envio de cobrança por e-mail.

### 2.11 Configurações da clínica

- Nome da clínica.
- Dias de funcionamento por dia da semana.
- Horário de abertura e fechamento.
- Intervalo de almoço.
- Duração padrão das sessões.
- Fuso horário (configura Hibernate, Jackson e toda a aplicação dinamicamente).
- Telefone/WhatsApp de contato.
- E-mail de contato.

### 2.12 Configurações de e-mail (SMTP)

- Configuração do servidor de envio.
- Teste de envio via interface.
- Log de e-mails enviados com status de entrega.

### 2.13 Usuários e permissões

- Cadastro, edição, inativação e reativação de usuários.
- Perfis: ADMIN, PROFISSIONAL, RECEPCIONISTA, VISUALIZADOR.
- Permissões de menu por perfil configuráveis pelo ADMIN.

### 2.14 Auditoria

- Log de criações, edições, cancelamentos, exclusões.
- Log de login e logout.
- Detalhamento de campos alterados (antes/depois).
- Filtros por módulo, ação, usuário e período.

---

## 3. Ordem sugerida para demonstração ao vivo

1. Login e visão do painel.
2. Configuração da clínica (horários, fuso, duração de sessão).
3. Cadastro de paciente.
4. Criação de plano de tratamento.
5. Agendamento de sessão avulsa e recorrente.
6. Solicitação de agendamento online + aprovação.
7. Marcar sessão como realizada + prontuário SOAP.
8. Módulo financeiro: baixa de lançamento e resumo.
9. Relatórios com exportação.
10. Auditoria: histórico de ações.

---

## 4. Observação

Este roteiro foi revisado em maio de 2026 com base nos controllers, services, templates e migrações presentes no projeto FisioGestão.
