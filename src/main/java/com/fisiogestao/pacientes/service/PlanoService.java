package com.fisiogestao.pacientes.service;

import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.PlanoSessaoStatus;
import com.fisiogestao.pacientes.dto.PacienteListaDTO;
import com.fisiogestao.pacientes.dto.PlanoFormDTO;
import com.fisiogestao.pacientes.dto.PlanoListItemDTO;
import com.fisiogestao.pacientes.dto.PlanoResumoDTO;
import com.fisiogestao.pacientes.repository.PlanoSessaoRepository;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanoService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PacienteService pacienteService;
    private final PlanoSessaoRepository planoSessaoRepository;
    private final PlanoSessaoService planoSessaoService;
    private final FinanceiroService financeiroService;

    @Transactional(readOnly = true)
    public List<PacienteListaDTO> listarPacientesComResumoPlano() {
        return pacienteService.listarAtivos().stream()
                .map(this::paraPacienteLista)
                .toList();
    }

    @Transactional(readOnly = true)
    public PacienteListaDTO buscarResumoDTO(UUID pacienteId) {
        return paraPacienteLista(pacienteService.buscarPorId(pacienteId));
    }

    @Transactional(readOnly = true)
    public PlanoResumoDTO buscarPlanoPorId(UUID planoId) {
        return planoSessaoRepository.findById(planoId)
                .map(this::paraPlanoResumo)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PlanoResumoDTO buscarPlanoPorPaciente(UUID pacienteId) {
        return planoSessaoRepository.findByClienteIdOrderByCreatedAtDesc(pacienteId).stream()
                .filter(plano -> plano.getStatus() != PlanoSessaoStatus.CANCELADO)
                .min(Comparator
                        .comparing((PlanoSessaoEntity plano) -> !planoSessaoService.isDisponivelParaUso(plano))
                        .thenComparing(plano -> sessoesRestantes(plano) <= 0)
                        .thenComparing(PlanoSessaoEntity::getDataValidade, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PlanoSessaoEntity::getCreatedAt))
                .map(this::paraPlanoResumo)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PlanoResumoDTO buscarUltimoPlanoIncluindoInativos(UUID pacienteId) {
        return planoSessaoRepository.findByClienteIdOrderByCreatedAtDesc(pacienteId).stream()
                .filter(plano -> plano.getStatus() != PlanoSessaoStatus.CANCELADO)
                .findFirst()
                .map(this::paraPlanoResumo)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PlanoListItemDTO> listarPorPaciente(UUID clienteId) {
        return planoSessaoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId).stream()
                .filter(p -> p.getStatus() != PlanoSessaoStatus.CANCELADO)
                .map(this::paraListItem)
                .toList();
    }

    /**
     * A anotação {@code @PreAuthorize} é a segunda linha de defesa:
     * garante que, mesmo que este método seja chamado fora do contexto HTTP
     * (ex.: via injeção direta por outro serviço), a verificação de role
     * ainda é enforced pelo Spring Security AOP.
     * A primeira linha de defesa é o {@code requestMatchers} em {@code SecurityConfig}.
     */
    @PreAuthorize("hasAnyRole('ADMIN_CLINICA', 'FINANCEIRO', 'RECEPCAO', 'FISIOTERAPEUTA')")
    @Transactional
    public PlanoSessaoEntity salvarOuAtualizar(UUID pacienteId, PlanoFormDTO form, String profissionalResponsavel) {
        ClienteEntity paciente = pacienteService.buscarPorId(pacienteId);
        boolean criandoNovoPlano = form.getPlanoId() == null;
        PlanoSessaoEntity plano;
        if (!criandoNovoPlano) {
            plano = planoSessaoRepository.findById(form.getPlanoId())
                    .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        } else {
            // Sem planoId explícito: cria sempre um novo plano
            plano = new PlanoSessaoEntity();
            plano.setCliente(paciente);
            plano.setSessoesConsumidas(0);
        }

        int total = form.getTotalSessoes() > 0 ? form.getTotalSessoes() : totalPorTipo(form.getTipoPlano());
        if (plano.getSessoesConsumidas() != null && plano.getSessoesConsumidas() > total) {
            throw new BusinessException("O total de sessoes nao pode ser menor que o consumo ja registrado.");
        }

        plano.setCliente(paciente);
        plano.setNomePlano(rotuloPlano(form.getTipoPlano(), total));
        plano.setSessoesContratadas(total);
        plano.setValorTotal(form.getValorTotal() == null ? BigDecimal.ZERO : form.getValorTotal());
        plano.setDataInicio(form.getInicioVigencia() == null ? LocalDate.now() : form.getInicioVigencia());
        plano.setDataValidade(form.getFimVigencia());
        plano.setStatus(status(form.getStatus(), plano, total));
        PlanoSessaoEntity salvo = planoSessaoRepository.save(plano);
        if (criandoNovoPlano) {
            registrarFaturamento(salvo, form.getNumeroParcelas(), profissionalResponsavel);
        }
        return salvo;
    }

    // Segunda linha de defesa (ver comentário completo em salvarOuAtualizar acima).
    @PreAuthorize("hasAnyRole('ADMIN_CLINICA', 'FINANCEIRO', 'RECEPCAO', 'FISIOTERAPEUTA')")
    @Transactional
    public PlanoSessaoEntity renovarPlano(UUID planoAnteriorId, PlanoFormDTO form, String profissionalResponsavel) {
        PlanoSessaoEntity anterior = planoSessaoRepository.findById(planoAnteriorId)
                .orElseThrow(() -> new BusinessException("Plano anterior não encontrado."));
        ClienteEntity paciente = anterior.getCliente();

        int total = form.getTotalSessoes() > 0 ? form.getTotalSessoes() : valorSeguro(anterior.getSessoesContratadas());
        PlanoSessaoEntity novo = new PlanoSessaoEntity();
        novo.setCliente(paciente);
        novo.setPlanoAnterior(anterior);
        novo.setSessoesConsumidas(0);
        novo.setNomePlano(rotuloPlano(form.getTipoPlano() != null ? form.getTipoPlano() : anterior.getNomePlano(), total));
        novo.setSessoesContratadas(total);
        novo.setValorTotal(form.getValorTotal() != null ? form.getValorTotal() : (anterior.getValorTotal() != null ? anterior.getValorTotal() : BigDecimal.ZERO));
        novo.setDataInicio(form.getInicioVigencia() != null ? form.getInicioVigencia() : LocalDate.now());
        novo.setDataValidade(form.getFimVigencia());
        novo.setStatus(PlanoSessaoStatus.ATIVO);
        PlanoSessaoEntity salvo = planoSessaoRepository.save(novo);
        registrarFaturamento(salvo, form.getNumeroParcelas(), profissionalResponsavel);
        return salvo;
    }

    private void registrarFaturamento(PlanoSessaoEntity plano, Integer numeroParcelas, String profissionalResponsavel) {
        int parcelas = numeroParcelas != null && numeroParcelas > 0 ? numeroParcelas : 1;
        if (parcelas > 1) {
            financeiroService.registrarReceitaPlanoParcelado(plano, parcelas, profissionalResponsavel);
        } else {
            financeiroService.registrarReceitaPlano(plano, profissionalResponsavel);
        }
    }

    public PlanoResumoDTO paraPlanoResumo(PlanoSessaoEntity plano) {
        int total = valorSeguro(plano.getSessoesContratadas());
        int usadas = valorSeguro(plano.getSessoesConsumidas());
        int restantes = Math.max(0, total - usadas);
        int percentual = total == 0 ? 0 : Math.min(100, Math.round((usadas * 100f) / total));
        return new PlanoResumoDTO(
                plano.getId(),
                plano.getNomePlano(),
                total,
                usadas,
                restantes,
                percentual,
                dinheiro(plano.getValorTotal()),
                corBarra(restantes),
                plano.getDataInicio(),
                plano.getDataValidade(),
                plano.getStatus() == null ? PlanoSessaoStatus.ATIVO.name() : plano.getStatus().name()
        );
    }

    private PlanoListItemDTO paraListItem(PlanoSessaoEntity plano) {
        int total = valorSeguro(plano.getSessoesContratadas());
        int usadas = valorSeguro(plano.getSessoesConsumidas());
        return new PlanoListItemDTO(
                plano.getId(),
                plano.getNomePlano(),
                plano.getStatus() == null ? PlanoSessaoStatus.ATIVO.name() : plano.getStatus().name(),
                plano.getDataInicio(),
                plano.getDataValidade(),
                total,
                usadas,
                Math.max(0, total - usadas),
                dinheiro(plano.getValorTotal())
        );
    }

    private PacienteListaDTO paraPacienteLista(ClienteEntity paciente) {
        PlanoResumoDTO plano = buscarPlanoPorPaciente(paciente.getId());
        boolean possuiPlano = plano != null;
        String nascimento = paciente.getDataNascimento() == null ? "Sem nascimento" : paciente.getDataNascimento().format(DATA_BR);
        String iniciais = iniciais(paciente.getNomeCompleto());
        String corAvatar = "var(--g-av-bg)";
        String corTextoAvatar = "var(--g-av-txt)";
        return new PacienteListaDTO(
                paciente.getId(),
                paciente.getNomeCompleto(),
                iniciais,
                nascimento,
                corAvatar,
                corTextoAvatar,
                possuiPlano,
                possuiPlano ? plano.sessoesUsadas() : 0,
                possuiPlano ? plano.totalSessoes() : 0,
                possuiPlano ? plano.sessoesRestantes() : 0,
                possuiPlano ? "plano" : "sem",
                paciente.getTelefone(),
                paciente.getEmail()
        );
    }

    private String iniciais(String nome) {
        if (nome == null || nome.isBlank()) {
            return "P";
        }
        String[] partes = nome.trim().split("\\s+");
        String primeira = partes[0].substring(0, 1);
        String segunda = partes.length > 1 ? partes[partes.length - 1].substring(0, 1) : "";
        return (primeira + segunda).toUpperCase(PT_BR);
    }

    private int sessoesRestantes(PlanoSessaoEntity plano) {
        return Math.max(0, valorSeguro(plano.getSessoesContratadas()) - valorSeguro(plano.getSessoesConsumidas()));
    }

    private int valorSeguro(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private String dinheiro(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor == null ? BigDecimal.ZERO : valor);
    }

    private String corBarra(int restantes) {
        if (restantes <= 1) {
            return "var(--g-pbar-crit)";
        }
        if (restantes <= 3) {
            return "var(--g-pbar-warn)";
        }
        return "var(--g-pbar-done)";
    }

    private int totalPorTipo(String tipoPlano) {
        if ("PLANO_5".equals(tipoPlano)) {
            return 5;
        }
        if ("PLANO_20".equals(tipoPlano)) {
            return 20;
        }
        return 10;
    }

    private String rotuloPlano(String tipoPlano, int total) {
        if (tipoPlano != null && tipoPlano.startsWith("PLANO_")) {
            return "Plano de " + total + " Sessões";
        }
        return tipoPlano == null || tipoPlano.isBlank() ? "Plano de " + total + " Sessões" : tipoPlano.trim();
    }

    private PlanoSessaoStatus status(String status, PlanoSessaoEntity plano, int total) {
        if (plano.getSessoesConsumidas() != null && plano.getSessoesConsumidas() >= total) {
            return PlanoSessaoStatus.CONCLUIDO;
        }
        try {
            return status == null || status.isBlank() ? PlanoSessaoStatus.ATIVO : PlanoSessaoStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            return PlanoSessaoStatus.ATIVO;
        }
    }
}
