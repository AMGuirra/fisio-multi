package com.fisiogestao.agenda;

import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.PlanoSessaoStatus;
import com.fisiogestao.pacientes.form.PlanoConsumoForm;
import com.fisiogestao.pacientes.service.PlanoSessaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendaControllerTest {

    @Mock
    private AgendaService agendaService;

    @Mock
    private PlanoSessaoService planoSessaoService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AgendaController controller;

    @Test
    void deveCriarPlanoEmJsonParaContinuarAgendamentoNoModal() {
        UUID clienteId = UUID.randomUUID();
        UUID planoId = UUID.randomUUID();
        PlanoConsumoForm form = formValido(clienteId);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "planoAgendaForm");

        ClienteEntity cliente = ClienteEntity.builder().nomeCompleto("Maria").build();
        cliente.setId(clienteId);
        PlanoSessaoEntity plano = PlanoSessaoEntity.builder()
                .cliente(cliente)
                .nomePlano("Plano mensal")
                .sessoesContratadas(10)
                .sessoesConsumidas(0)
                .valorTotal(new BigDecimal("500.00"))
                .dataInicio(LocalDate.of(2026, 10, 1))
                .status(PlanoSessaoStatus.ATIVO)
                .build();
        plano.setId(planoId);

        when(authentication.getName()).thenReturn("recepcao");
        when(planoSessaoService.criarViaCrud(form, "recepcao")).thenReturn(plano);
        when(planoSessaoService.calcularSessoesDisponiveisParaAgenda(plano)).thenReturn(10);

        ResponseEntity<?> response = controller.criarPlanoPelaAgendaJson(form, bindingResult, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(Boolean.TRUE, body.get("success"));
        assertEquals(planoId, body.get("id"));
        assertEquals(clienteId, body.get("clienteId"));
        assertEquals("Plano mensal", body.get("nomePlano"));
        assertEquals(10, body.get("sessoesLivres"));
    }

    @Test
    void naoDeveCriarPlanoQuandoFormularioDoModalForInvalido() {
        PlanoConsumoForm form = new PlanoConsumoForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "planoAgendaForm");
        bindingResult.addError(new FieldError(
                "planoAgendaForm",
                "nomePlano",
                "Informe o nome do plano de tratamento."
        ));

        ResponseEntity<?> response = controller.criarPlanoPelaAgendaJson(form, bindingResult, authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(Boolean.FALSE, body.get("success"));
        assertEquals("Informe o nome do plano de tratamento.", body.get("error"));
        verify(planoSessaoService, never()).criarViaCrud(form, "recepcao");
    }

    private PlanoConsumoForm formValido(UUID clienteId) {
        PlanoConsumoForm form = new PlanoConsumoForm();
        form.setClienteId(clienteId);
        form.setNomePlano("Plano mensal");
        form.setSessoesContratadas(10);
        form.setSessoesConsumidas(0);
        form.setValorTotal(new BigDecimal("500.00"));
        form.setDataInicio(LocalDate.of(2026, 10, 1));
        form.setStatus(PlanoSessaoStatus.ATIVO);
        form.setNumeroParcelas(1);
        return form;
    }
}
