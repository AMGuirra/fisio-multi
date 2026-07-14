package com.fisiogestao.pacientes.service;

import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.PlanoSessaoStatus;
import com.fisiogestao.pacientes.dto.PlanoFormDTO;
import com.fisiogestao.pacientes.repository.PlanoSessaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanoServiceTest {

    @Mock
    private PacienteService pacienteService;

    @Mock
    private PlanoSessaoRepository planoSessaoRepository;

    @Mock
    private PlanoSessaoService planoSessaoService;

    @Mock
    private FinanceiroService financeiroService;

    @InjectMocks
    private PlanoService planoService;

    // -----------------------------------------------------------------------
    // salvarOuAtualizar — criação (planoId nulo) deve faturar
    // -----------------------------------------------------------------------

    @Test
    void criacaoDeNovoPlanoDeveRegistrarReceitaUnicaQuandoParcelasNaoInformadas() {
        UUID pacienteId = UUID.randomUUID();
        PlanoFormDTO form = formNovoPlano("PLANO_10", 10, "500.00");

        when(pacienteService.buscarPorId(pacienteId)).thenReturn(cliente("Maria"));
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> {
            PlanoSessaoEntity p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        planoService.salvarOuAtualizar(pacienteId, form, "Dra. Ana");

        verify(financeiroService).registrarReceitaPlano(any(PlanoSessaoEntity.class), eq("Dra. Ana"));
        verify(financeiroService, never()).registrarReceitaPlanoParcelado(any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }

    @Test
    void criacaoDeNovoPlanoDeveRegistrarReceitaParceladaQuandoParcelasMaiorQueUm() {
        UUID pacienteId = UUID.randomUUID();
        PlanoFormDTO form = formNovoPlano("PLANO_10", 10, "600.00");
        form.setNumeroParcelas(3);

        when(pacienteService.buscarPorId(pacienteId)).thenReturn(cliente("Maria"));
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> {
            PlanoSessaoEntity p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        planoService.salvarOuAtualizar(pacienteId, form, "Dra. Ana");

        verify(financeiroService).registrarReceitaPlanoParcelado(any(PlanoSessaoEntity.class), eq(3), eq("Dra. Ana"));
        verify(financeiroService, never()).registrarReceitaPlano(any(), any());
    }

    // -----------------------------------------------------------------------
    // salvarOuAtualizar — edição (planoId preenchido) não deve refaturar
    // -----------------------------------------------------------------------

    @Test
    void edicaoDePlanoExistenteNaoDeveGerarNovoLancamentoFinanceiro() {
        UUID pacienteId = UUID.randomUUID();
        ClienteEntity clienteObj = cliente("Maria");
        PlanoSessaoEntity existente = plano(clienteObj, 10, 2, "500.00");

        PlanoFormDTO form = formNovoPlano("PLANO_10", 10, "500.00");
        form.setPlanoId(existente.getId());
        form.setStatus("ATIVO");

        when(pacienteService.buscarPorId(pacienteId)).thenReturn(clienteObj);
        when(planoSessaoRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        planoService.salvarOuAtualizar(pacienteId, form, "Dra. Ana");

        verify(financeiroService, never()).registrarReceitaPlano(any(), any());
        verify(financeiroService, never()).registrarReceitaPlanoParcelado(any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }

    // -----------------------------------------------------------------------
    // renovarPlano — sempre fatura, pois sempre cria uma entidade nova
    // -----------------------------------------------------------------------

    @Test
    void renovarPlanoDeveRegistrarReceitaUnicaQuandoParcelasNaoInformadas() {
        ClienteEntity clienteObj = cliente("Maria");
        PlanoSessaoEntity anterior = plano(clienteObj, 10, 10, "500.00");

        PlanoFormDTO form = formNovoPlano("PLANO_10", 10, "500.00");

        when(planoSessaoRepository.findById(anterior.getId())).thenReturn(Optional.of(anterior));
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> {
            PlanoSessaoEntity p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        planoService.renovarPlano(anterior.getId(), form, "Dra. Ana");

        verify(financeiroService).registrarReceitaPlano(any(PlanoSessaoEntity.class), eq("Dra. Ana"));
    }

    @Test
    void renovarPlanoDeveRegistrarReceitaParceladaQuandoParcelasMaiorQueUm() {
        ClienteEntity clienteObj = cliente("Maria");
        PlanoSessaoEntity anterior = plano(clienteObj, 10, 10, "600.00");

        PlanoFormDTO form = formNovoPlano("PLANO_10", 10, "600.00");
        form.setNumeroParcelas(3);

        when(planoSessaoRepository.findById(anterior.getId())).thenReturn(Optional.of(anterior));
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> {
            PlanoSessaoEntity p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        planoService.renovarPlano(anterior.getId(), form, "Dra. Ana");

        verify(financeiroService).registrarReceitaPlanoParcelado(any(PlanoSessaoEntity.class), eq(3), eq("Dra. Ana"));
    }

    private PlanoFormDTO formNovoPlano(String tipoPlano, int totalSessoes, String valorTotal) {
        PlanoFormDTO form = new PlanoFormDTO();
        form.setTipoPlano(tipoPlano);
        form.setTotalSessoes(totalSessoes);
        form.setValorTotal(new BigDecimal(valorTotal));
        form.setInicioVigencia(LocalDate.now());
        form.setFimVigencia(LocalDate.now().plusMonths(1));
        return form;
    }

    private ClienteEntity cliente(String nome) {
        ClienteEntity c = ClienteEntity.builder().nomeCompleto(nome).ativo(true).build();
        c.setId(UUID.randomUUID());
        return c;
    }

    private PlanoSessaoEntity plano(ClienteEntity cliente, int contratadas, int consumidas, String valor) {
        PlanoSessaoEntity p = PlanoSessaoEntity.builder()
                .cliente(cliente)
                .nomePlano("Plano de " + contratadas + " Sessões")
                .sessoesContratadas(contratadas)
                .sessoesConsumidas(consumidas)
                .valorTotal(new BigDecimal(valor))
                .dataInicio(LocalDate.now().minusDays(10))
                .dataValidade(LocalDate.now().plusMonths(3))
                .status(PlanoSessaoStatus.ATIVO)
                .build();
        p.setId(UUID.randomUUID());
        return p;
    }
}
