package com.fisiogestao.agenda.service;

import com.fisiogestao.agenda.BloqueioAgendaEntity;
import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.form.BloqueioAgendaCadastroForm;
import com.fisiogestao.agenda.repository.BloqueioAgendaRepository;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueioAgendaServiceTest {

    @Mock
    private BloqueioAgendaRepository bloqueioAgendaRepository;

    @Mock
    private ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private BloqueioAgendaService bloqueioAgendaService;

    @Test
    void deveSalvarBloqueioDiaInteiroCobrindoTodaAData() {
        LocalDate data = LocalDate.now().plusDays(3);
        ZoneId zoneId = ZoneId.of("America/Campo_Grande");
        BloqueioAgendaCadastroForm form = new BloqueioAgendaCadastroForm();
        form.setDataInicial(data);
        form.setDataFinal(data);
        form.setBloqueioDiaInteiro(true);
        form.setMotivo("Feriado");

        when(configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc())
                .thenReturn(Optional.of(criarConfiguracao()));
        when(bloqueioAgendaRepository.findByStatusAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(any(), any(), any()))
                .thenReturn(List.of());
        when(sessaoRepository.existsByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(any(), any(), any()))
                .thenReturn(false);
        when(bloqueioAgendaRepository.save(any(BloqueioAgendaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        bloqueioAgendaService.criar(form, "admin");

        ArgumentCaptor<BloqueioAgendaEntity> captor = ArgumentCaptor.forClass(BloqueioAgendaEntity.class);
        verify(bloqueioAgendaRepository).save(captor.capture());
        BloqueioAgendaEntity salvo = captor.getValue();

        assertEquals(data.atStartOfDay(zoneId).toOffsetDateTime(), salvo.getDataHoraInicio());
        assertEquals(data.atTime(LocalTime.MAX).atZone(zoneId).toOffsetDateTime(), salvo.getDataHoraFim());
        assertEquals(true, salvo.getDiaInteiro());
    }

    private ConfiguracaoClinicaEntity criarConfiguracao() {
        return ConfiguracaoClinicaEntity.builder()
                .nomeClinica("Clinica Teste")
                .diasFuncionamento("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY")
                .horarioAbertura(LocalTime.of(8, 0))
                .horarioFechamento(LocalTime.of(18, 0))
                .duracaoSessaoMinutos(50)
                .timezoneId("America/Campo_Grande")
                .ativo(true)
                .build();
    }
}
