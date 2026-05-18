package com.fisiogestao.portal;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.TipoLancamentoFinanceiro;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.PlanoSessaoStatus;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.pacientes.repository.PlanoSessaoRepository;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.listaespera.ListaEsperaEntity;
import com.fisiogestao.listaespera.ListaEsperaService;
import com.fisiogestao.listaespera.PeriodoPreferido;
import com.fisiogestao.listaespera.StatusListaEspera;
import com.fisiogestao.solicitacoes.form.AgendamentoOnlineForm;
import com.fisiogestao.solicitacoes.service.SolicitacaoAgendamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequestMapping("/paciente")
@RequiredArgsConstructor
public class PortalPacienteController {

    private static final DateTimeFormatter GCAL_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final int HORAS_MINIMAS_CANCELAMENTO = 24;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final ClienteRepository clienteRepository;
    private final SessaoRepository sessaoRepository;
    private final PlanoSessaoRepository planoSessaoRepository;
    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;
    private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    private final AgendaService agendaService;
    private final SolicitacaoAgendamentoService solicitacaoAgendamentoService;
    private final ListaEsperaService listaEsperaService;

    @GetMapping("/{token}")
    public String portal(@PathVariable UUID token, Model model) {
        ClienteEntity paciente = clienteRepository.findByPortalToken(token).orElse(null);

        if (paciente == null) {
            return "portal/token-invalido";
        }

        ConfiguracaoClinicaEntity clinica = configuracaoClinicaRepository
                .findFirstByAtivoTrueOrderByCreatedAtAsc()
                .orElse(null);

        String nomeClinica = clinica != null ? clinica.getNomeClinica() : "Fisioterapia";
        OffsetDateTime agora = ClinicTimezoneHolder.now();

        List<SessaoEntity> proximasRaw = sessaoRepository
                .findByClienteIdAndStatusAndDataHoraInicioAfterOrderByDataHoraInicioAsc(
                        paciente.getId(), SessaoStatus.AGENDADA, agora);

        List<SessaoPortalView> proximasSessoes = proximasRaw.stream()
                .map(s -> new SessaoPortalView(
                        s,
                        buildCalendarUrl(s, nomeClinica),
                        podeCancelar(s, agora),
                        HORAS_MINIMAS_CANCELAMENTO))
                .toList();

        List<SessaoEntity> historicoSessoes = sessaoRepository
                .findTop10ByClienteIdAndStatusNotOrderByDataHoraInicioDesc(
                        paciente.getId(), SessaoStatus.AGENDADA);

        List<PlanoSessaoEntity> planos = planoSessaoRepository
                .findByClienteIdOrderByCreatedAtDesc(paciente.getId());

        List<PlanoSessaoEntity> planosAtivos = planos.stream()
                .filter(p -> p.getStatus() == PlanoSessaoStatus.ATIVO)
                .toList();

        List<PlanoSessaoEntity> planosEncerrados = planos.stream()
                .filter(p -> p.getStatus() != PlanoSessaoStatus.ATIVO)
                .toList();

        int saldoSessoesAtivas = planosAtivos.stream()
                .mapToInt(this::sessoesRestantes)
                .sum();
        int sessoesContratadasAtivas = planosAtivos.stream()
                .mapToInt(p -> p.getSessoesContratadas() != null ? p.getSessoesContratadas() : 0)
                .sum();
        int sessoesConsumidasAtivas = planosAtivos.stream()
                .mapToInt(p -> p.getSessoesConsumidas() != null ? p.getSessoesConsumidas() : 0)
                .sum();
        int progressoTratamento = sessoesContratadasAtivas > 0
                ? Math.min(100, Math.max(0, sessoesConsumidasAtivas * 100 / sessoesContratadasAtivas))
                : 0;
        long sessoesRealizadas = sessaoRepository.countByClienteIdAndStatus(
                paciente.getId(), SessaoStatus.REALIZADA);
        long faltasFaturadas = sessaoRepository.countByClienteIdAndStatus(
                paciente.getId(), SessaoStatus.FALTA_FATURADA);

        List<LancamentoFinanceiroEntity> recibos = lancamentoFinanceiroRepository
                .findByClienteIdAndTipoAndStatusOrderByDataPagamentoDescDataCompetenciaDescCreatedAtDesc(
                        paciente.getId(), TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PAGO);

        String whatsappUrl = null;
        if (clinica != null && clinica.getTelefoneWhatsapp() != null
                && !clinica.getTelefoneWhatsapp().isBlank()) {
            String soDigitos = clinica.getTelefoneWhatsapp().replaceAll("[^0-9]", "");
            whatsappUrl = "https://wa.me/" + soDigitos;
        }

        model.addAttribute("paciente", paciente);
        model.addAttribute("token", token);
        model.addAttribute("clinica", clinica);
        model.addAttribute("whatsappUrl", whatsappUrl);
        model.addAttribute("proximasSessoes", proximasSessoes);
        model.addAttribute("historicoSessoes", historicoSessoes);
        model.addAttribute("planosAtivos", planosAtivos);
        model.addAttribute("planosEncerrados", planosEncerrados);
        model.addAttribute("saldoSessoesAtivas", saldoSessoesAtivas);
        model.addAttribute("sessoesContratadasAtivas", sessoesContratadasAtivas);
        model.addAttribute("sessoesConsumidasAtivas", sessoesConsumidasAtivas);
        model.addAttribute("progressoTratamento", progressoTratamento);
        model.addAttribute("sessoesRealizadas", sessoesRealizadas);
        model.addAttribute("faltasFaturadas", faltasFaturadas);
        model.addAttribute("recibos", recibos);
        model.addAttribute("horasMinimasCancelamento", HORAS_MINIMAS_CANCELAMENTO);
        List<ListaEsperaEntity> listaEsperaEntradas = listaEsperaService.listarPorCliente(paciente.getId());
        boolean naListaEspera = listaEsperaEntradas.stream()
                .anyMatch(e -> e.getStatus() == StatusListaEspera.AGUARDANDO);

        model.addAttribute("mapsUrl", buildMapsUrl(clinica));
        model.addAttribute("agendamentoForm", montarAgendamentoForm(paciente));
        model.addAttribute("listaEsperaEntradas", listaEsperaEntradas);
        model.addAttribute("periodoPreferidoOpcoes", PeriodoPreferido.values());
        model.addAttribute("naListaEspera", naListaEspera);

        return "portal/index";
    }

    @PostMapping("/{token}/agendamento")
    public String solicitarAgendamento(
            @PathVariable UUID token,
            @ModelAttribute("agendamentoForm") AgendamentoOnlineForm form,
            RedirectAttributes redirectAttributes) {

        ClienteEntity paciente = clienteRepository.findByPortalToken(token).orElse(null);
        if (paciente == null) {
            return "redirect:/paciente/" + token;
        }

        preencherDadosPaciente(form, paciente);

        try {
            solicitacaoAgendamentoService.criarSolicitacaoPublica(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Solicitacao enviada. A equipe vai confirmar o novo horario.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/paciente/" + token;
    }

    @PostMapping("/{token}/sessao/{sessaoId}/confirmar")
    public String confirmarPresenca(
            @PathVariable UUID token,
            @PathVariable UUID sessaoId,
            RedirectAttributes redirectAttributes) {

        ClienteEntity paciente = clienteRepository.findByPortalToken(token).orElse(null);
        if (paciente == null) {
            return "redirect:/paciente/" + token;
        }

        sessaoRepository.findByIdAndClienteIdAndStatus(sessaoId, paciente.getId(), SessaoStatus.AGENDADA)
                .ifPresent(sessao -> {
                    sessao.setConfirmadoPeloPaciente(true);
                    sessaoRepository.save(sessao);
                });

        redirectAttributes.addFlashAttribute("successMessage",
                "Presenca confirmada! Te esperamos na sessao.");
        return "redirect:/paciente/" + token;
    }

    @PostMapping("/{token}/sessao/{sessaoId}/cancelar")
    public String cancelarSessao(
            @PathVariable UUID token,
            @PathVariable UUID sessaoId,
            RedirectAttributes redirectAttributes) {

        ClienteEntity paciente = clienteRepository.findByPortalToken(token).orElse(null);
        if (paciente == null) {
            return "redirect:/paciente/" + token;
        }

        SessaoEntity sessao = sessaoRepository
                .findByIdAndClienteIdAndStatus(sessaoId, paciente.getId(), SessaoStatus.AGENDADA)
                .orElse(null);

        if (sessao == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sessao nao encontrada para cancelamento.");
            return "redirect:/paciente/" + token;
        }

        if (!podeCancelar(sessao, ClinicTimezoneHolder.now())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cancelamentos pelo portal precisam ser feitos com pelo menos "
                            + HORAS_MINIMAS_CANCELAMENTO + " horas de antecedencia.");
            return "redirect:/paciente/" + token;
        }

        try {
            agendaService.cancelarSessao(sessaoId, "Cancelamento solicitado pelo paciente via portal.");
            redirectAttributes.addFlashAttribute("successMessage", "Sessao cancelada com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/paciente/" + token;
    }

    @PostMapping("/{token}/lista-espera/entrar")
    public String entrarNaListaEspera(
            @PathVariable UUID token,
            @RequestParam(value = "periodo", required = false) PeriodoPreferido periodo,
            @RequestParam(value = "diasSemana", required = false) String diasSemana,
            @RequestParam(value = "observacoes", required = false) String observacoes,
            RedirectAttributes redirectAttributes) {

        ClienteEntity paciente = clienteRepository.findByPortalToken(token).orElse(null);
        if (paciente == null) {
            return "redirect:/paciente/" + token;
        }

        try {
            listaEsperaService.entrarNaFila(paciente, periodo, diasSemana, observacoes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Você entrou na lista de espera! Entraremos em contato quando houver uma vaga.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/paciente/" + token;
    }

    @PostMapping("/{token}/lista-espera/{entradaId}/cancelar")
    public String sairDaListaEspera(
            @PathVariable UUID token,
            @PathVariable UUID entradaId,
            RedirectAttributes redirectAttributes) {

        ClienteEntity paciente = clienteRepository.findByPortalToken(token).orElse(null);
        if (paciente == null) {
            return "redirect:/paciente/" + token;
        }

        try {
            listaEsperaService.cancelarPeloPaciente(entradaId, paciente.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Você saiu da lista de espera.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/paciente/" + token;
    }

    @PostMapping("/{token}/sessao/{sessaoId}/feedback-eva")
    public String registrarFeedbackEva(
            @PathVariable UUID token,
            @PathVariable UUID sessaoId,
            @RequestParam("eva") Integer eva,
            @RequestParam(value = "obs", required = false) String obs,
            RedirectAttributes redirectAttributes) {

        ClienteEntity paciente = clienteRepository.findByPortalToken(token).orElse(null);
        if (paciente == null) {
            return "redirect:/paciente/" + token;
        }

        if (eva == null || eva < 0 || eva > 10) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nota EVA inválida. Informe um valor entre 0 e 10.");
            return "redirect:/paciente/" + token;
        }

        sessaoRepository.findById(sessaoId).ifPresent(sessao -> {
            boolean doMesmoPaciente = sessao.getCliente().getId().equals(paciente.getId());
            boolean statusPermite = sessao.getStatus() == com.fisiogestao.agenda.SessaoStatus.REALIZADA
                    || sessao.getStatus() == com.fisiogestao.agenda.SessaoStatus.FALTA_FATURADA;
            boolean semFeedback = sessao.getFeedbackEvaPaciente() == null;
            if (doMesmoPaciente && statusPermite && semFeedback) {
                sessao.setFeedbackEvaPaciente(eva);
                if (obs != null && !obs.isBlank()) {
                    sessao.setFeedbackObsPaciente(obs.trim());
                }
                sessaoRepository.save(sessao);
            }
        });

        redirectAttributes.addFlashAttribute("successMessage", "Obrigado pelo seu feedback!");
        return "redirect:/paciente/" + token;
    }

    @GetMapping("/{token}/recibos/{lancamentoId}")
    public ResponseEntity<String> baixarRecibo(
            @PathVariable UUID token,
            @PathVariable UUID lancamentoId) {

        ClienteEntity paciente = clienteRepository.findByPortalToken(token).orElse(null);
        if (paciente == null) {
            return ResponseEntity.notFound().build();
        }

        LancamentoFinanceiroEntity lancamento = lancamentoFinanceiroRepository
                .findByIdAndClienteIdAndTipoAndStatus(
                        lancamentoId,
                        paciente.getId(),
                        TipoLancamentoFinanceiro.RECEITA,
                        StatusLancamentoFinanceiro.PAGO)
                .orElse(null);

        if (lancamento == null) {
            return ResponseEntity.notFound().build();
        }

        String nomeArquivo = "recibo-" + lancamento.getId() + ".txt";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(nomeArquivo, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(gerarReciboTexto(paciente, lancamento));
    }

    private String buildCalendarUrl(SessaoEntity sessao, String nomeClinica) {
        String inicio = sessao.getDataHoraInicio().toInstant().atOffset(ZoneOffset.UTC).format(GCAL_FMT);
        String fim = sessao.getDataHoraFim().toInstant().atOffset(ZoneOffset.UTC).format(GCAL_FMT);
        String titulo = URLEncoder.encode("Fisioterapia - " + nomeClinica, StandardCharsets.UTF_8);
        return "https://calendar.google.com/calendar/render?action=TEMPLATE"
                + "&text=" + titulo
                + "&dates=" + inicio + "/" + fim;
    }

    private boolean podeCancelar(SessaoEntity sessao, OffsetDateTime agora) {
        return !sessao.getDataHoraInicio().isBefore(agora.plusHours(HORAS_MINIMAS_CANCELAMENTO));
    }

    private int sessoesRestantes(PlanoSessaoEntity plano) {
        int contratadas = plano.getSessoesContratadas() != null ? plano.getSessoesContratadas() : 0;
        int consumidas = plano.getSessoesConsumidas() != null ? plano.getSessoesConsumidas() : 0;
        return Math.max(contratadas - consumidas, 0);
    }

    private AgendamentoOnlineForm montarAgendamentoForm(ClienteEntity paciente) {
        AgendamentoOnlineForm form = new AgendamentoOnlineForm();
        preencherDadosPaciente(form, paciente);
        form.setLocalAtendimento("CLINICA");
        return form;
    }

    private void preencherDadosPaciente(AgendamentoOnlineForm form, ClienteEntity paciente) {
        if (form.getNomePaciente() == null || form.getNomePaciente().isBlank()) {
            form.setNomePaciente(paciente.getNomeCompleto());
        }
        if (form.getTelefone() == null || form.getTelefone().isBlank()) {
            form.setTelefone(paciente.getTelefone());
        }
        if (form.getEmail() == null || form.getEmail().isBlank()) {
            form.setEmail(paciente.getEmail());
        }
        if (form.getCpf() == null || form.getCpf().isBlank()) {
            form.setCpf(paciente.getCpf());
        }
        if (form.getCep() == null || form.getCep().isBlank()) {
            form.setCep(paciente.getCep());
        }
        if (form.getRua() == null || form.getRua().isBlank()) {
            form.setRua(paciente.getRua());
        }
        if (form.getNumero() == null || form.getNumero().isBlank()) {
            form.setNumero(paciente.getNumero());
        }
        if (form.getBairro() == null || form.getBairro().isBlank()) {
            form.setBairro(paciente.getBairro());
        }
        if (form.getCidade() == null || form.getCidade().isBlank()) {
            form.setCidade(paciente.getCidade());
        }
        if (form.getEstado() == null || form.getEstado().isBlank()) {
            form.setEstado(paciente.getEstado());
        }
        if (form.getLocalAtendimento() == null || form.getLocalAtendimento().isBlank()) {
            form.setLocalAtendimento("CLINICA");
        }
    }

    private String buildMapsUrl(ConfiguracaoClinicaEntity clinica) {
        if (clinica == null) {
            return "https://www.google.com/maps/search/?api=1&query=clinica+de+fisioterapia";
        }
        String endereco = buildEnderecoCompleto(clinica);
        String termo = !endereco.isBlank() ? endereco : (clinica.getNomeClinica() != null ? clinica.getNomeClinica() : "clinica de fisioterapia");
        return "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(termo, StandardCharsets.UTF_8);
    }

    private String buildEnderecoCompleto(ConfiguracaoClinicaEntity clinica) {
        StringBuilder sb = new StringBuilder();
        if (clinica.getLogradouro() != null && !clinica.getLogradouro().isBlank()) {
            sb.append(clinica.getLogradouro().trim());
        }
        if (clinica.getNumero() != null && !clinica.getNumero().isBlank()) {
            sb.append(sb.isEmpty() ? "" : ", ").append(clinica.getNumero().trim());
        }
        if (clinica.getBairro() != null && !clinica.getBairro().isBlank()) {
            sb.append(sb.isEmpty() ? "" : ", ").append(clinica.getBairro().trim());
        }
        if (clinica.getCidade() != null && !clinica.getCidade().isBlank()) {
            sb.append(sb.isEmpty() ? "" : ", ").append(clinica.getCidade().trim());
        }
        if (clinica.getEstado() != null && !clinica.getEstado().isBlank()) {
            sb.append(sb.isEmpty() ? "" : " - ").append(clinica.getEstado().trim().toUpperCase());
        }
        return sb.toString();
    }

    private String gerarReciboTexto(ClienteEntity paciente, LancamentoFinanceiroEntity lancamento) {
        NumberFormat moeda = NumberFormat.getCurrencyInstance(PT_BR);
        String dataPagamento = lancamento.getDataPagamento() != null
                ? lancamento.getDataPagamento().atZoneSameInstant(ClinicTimezoneHolder.zoneId())
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "Pagamento registrado";
        String competencia = lancamento.getDataCompetencia()
                .atZoneSameInstant(ClinicTimezoneHolder.zoneId())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        BigDecimal valor = lancamento.getValor() != null ? lancamento.getValor() : BigDecimal.ZERO;

        String formaPagamento = lancamento.getMetodoPagamento() != null
                ? lancamento.getMetodoPagamento().getLabel()
                : "Não informada";

        return """
                RECIBO DE PAGAMENTO

                Paciente: %s
                Descricao: %s
                Categoria: %s
                Competencia: %s
                Data do pagamento: %s
                Forma de pagamento: %s
                Valor: %s

                Este recibo foi emitido pelo portal do paciente.
                """.formatted(
                paciente.getNomeCompleto(),
                lancamento.getDescricao(),
                lancamento.getCategoria(),
                competencia,
                dataPagamento,
                formaPagamento,
                moeda.format(valor));
    }
}
