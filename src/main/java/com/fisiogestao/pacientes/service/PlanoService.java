package com.fisiogestao.pacientes.service;

import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.PacoteSessaoStatus;
import com.fisiogestao.pacientes.dto.PacienteListaDTO;
import com.fisiogestao.pacientes.dto.PlanoFormDTO;
import com.fisiogestao.pacientes.dto.PlanoResumoDTO;
import com.fisiogestao.pacientes.repository.PacoteSessaoRepository;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
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
    private final PacoteSessaoRepository pacoteSessaoRepository;

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
    public PlanoResumoDTO buscarPlanoPorPaciente(UUID pacienteId) {
        return pacoteSessaoRepository.findByClienteIdOrderByCreatedAtDesc(pacienteId).stream()
                .filter(pacote -> pacote.getStatus() == PacoteSessaoStatus.ATIVO)
                .min(Comparator
                        .comparing((PacoteSessaoEntity pacote) -> sessoesRestantes(pacote) <= 0)
                        .thenComparing(PacoteSessaoEntity::getDataValidade, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PacoteSessaoEntity::getCreatedAt))
                .map(this::paraPlanoResumo)
                .orElse(null);
    }

    @Transactional
    public PacoteSessaoEntity salvarOuAtualizar(UUID pacienteId, PlanoFormDTO form) {
        ClienteEntity paciente = pacienteService.buscarPorId(pacienteId);
        PacoteSessaoEntity pacote = pacoteSessaoRepository.findByClienteIdOrderByCreatedAtDesc(pacienteId).stream()
                .filter(item -> item.getStatus() == PacoteSessaoStatus.ATIVO)
                .findFirst()
                .orElseGet(() -> {
                    PacoteSessaoEntity novo = new PacoteSessaoEntity();
                    novo.setCliente(paciente);
                    novo.setSessoesConsumidas(0);
                    return novo;
                });

        int total = form.getTotalSessoes() > 0 ? form.getTotalSessoes() : totalPorTipo(form.getTipoPlano());
        if (pacote.getSessoesConsumidas() != null && pacote.getSessoesConsumidas() > total) {
            throw new BusinessException("O total de sessoes nao pode ser menor que o consumo ja registrado.");
        }

        pacote.setCliente(paciente);
        pacote.setNomePacote(rotuloPlano(form.getTipoPlano(), total));
        pacote.setSessoesContratadas(total);
        pacote.setValorTotal(form.getValorTotal() == null ? BigDecimal.ZERO : form.getValorTotal());
        pacote.setDataInicio(form.getInicioVigencia() == null ? java.time.LocalDate.now() : form.getInicioVigencia());
        pacote.setDataValidade(form.getFimVigencia());
        pacote.setStatus(status(form.getStatus(), pacote, total));
        return pacoteSessaoRepository.save(pacote);
    }

    public PlanoResumoDTO paraPlanoResumo(PacoteSessaoEntity pacote) {
        int total = valorSeguro(pacote.getSessoesContratadas());
        int usadas = valorSeguro(pacote.getSessoesConsumidas());
        int restantes = Math.max(0, total - usadas);
        int percentual = total == 0 ? 0 : Math.min(100, Math.round((usadas * 100f) / total));
        return new PlanoResumoDTO(
                pacote.getId(),
                pacote.getNomePacote(),
                total,
                usadas,
                restantes,
                percentual,
                dinheiro(pacote.getValorTotal()),
                corBarra(restantes),
                pacote.getDataInicio(),
                pacote.getDataValidade(),
                pacote.getStatus() == null ? PacoteSessaoStatus.ATIVO.name() : pacote.getStatus().name()
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
                possuiPlano ? "pacote" : "sem",
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

    private int sessoesRestantes(PacoteSessaoEntity pacote) {
        return Math.max(0, valorSeguro(pacote.getSessoesContratadas()) - valorSeguro(pacote.getSessoesConsumidas()));
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
        if ("PACOTE_5".equals(tipoPlano)) {
            return 5;
        }
        if ("PACOTE_20".equals(tipoPlano)) {
            return 20;
        }
        return 10;
    }

    private String rotuloPlano(String tipoPlano, int total) {
        if (tipoPlano != null && tipoPlano.startsWith("PACOTE_")) {
            return "Pacote de " + total + " Sessões";
        }
        return tipoPlano == null || tipoPlano.isBlank() ? "Pacote de " + total + " Sessões" : tipoPlano.trim();
    }

    private PacoteSessaoStatus status(String status, PacoteSessaoEntity pacote, int total) {
        if (pacote.getSessoesConsumidas() != null && pacote.getSessoesConsumidas() >= total) {
            return PacoteSessaoStatus.CONCLUIDO;
        }
        try {
            return status == null || status.isBlank() ? PacoteSessaoStatus.ATIVO : PacoteSessaoStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            return PacoteSessaoStatus.ATIVO;
        }
    }
}
