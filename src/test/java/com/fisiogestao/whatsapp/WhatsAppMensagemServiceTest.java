package com.fisiogestao.whatsapp;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.pacientes.ClienteEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WhatsAppMensagemServiceTest {

    private WhatsAppMensagemService service;
    private final LocalDate hoje = ClinicTimezoneHolder.today();

    @BeforeEach
    void setUp() {
        ConfiguracaoClinicaRepository repository = mock(ConfiguracaoClinicaRepository.class);
        ConfiguracaoClinicaEntity clinica = new ConfiguracaoClinicaEntity();
        clinica.setNomeClinica("Clínica Movimento");
        when(repository.findFirstByAtivoTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(clinica));
        service = new WhatsAppMensagemService(repository);
        ReflectionTestUtils.setField(service, "baseUrl", "https://fisio.exemplo.com/");
    }

    @Test
    void deveGerarLinksDaSessaoAgendadaComMensagensPreConfiguradas() {
        SessaoEntity sessao = sessao(paciente("(47) 99999-8888"), hoje.plusDays(1), SessaoStatus.AGENDADA);

        Map<UUID, WhatsAppMensagemService.LinksSessao> links = service.linksSessoes(List.of(sessao));
        WhatsAppMensagemService.LinksSessao wa = links.get(sessao.getId());

        String data = String.format("%02d/%02d/%d", hoje.plusDays(1).getDayOfMonth(), hoje.plusDays(1).getMonthValue(), hoje.plusDays(1).getYear());
        assertTrue(wa.agendamento().startsWith("https://wa.me/5547999998888?text="));
        assertEquals("Olá Maria, seu agendamento na Clínica Movimento foi realizado com sucesso para "
                + data + " às 09:30.", texto(wa.agendamento()));
        assertEquals("Olá Maria, lembramos da sua sessão de Fisioterapia agendada para amanhã, "
                + data + " às 09:30, na Clínica Movimento. Te esperamos!", texto(wa.lembrete()));
        assertEquals("Olá Maria! Confirmamos sua sessão amanhã, " + data + " às 09:30? Responda com:\n"
                + "1 - Sim, confirmo\n2 - Não poderei ir (preciso reagendar)", texto(wa.confirmacao()));
        assertTrue(texto(wa.portal()).endsWith("https://fisio.exemplo.com/paciente/" + sessao.getCliente().getPortalToken()));
    }

    @Test
    void naoDeveGerarLinksSemTelefoneOuForaDoStatusAgendada() {
        SessaoEntity semTelefone = sessao(paciente(null), hoje, SessaoStatus.AGENDADA);
        SessaoEntity realizada = sessao(paciente("47999998888"), hoje, SessaoStatus.REALIZADA);

        assertTrue(service.linksSessoes(List.of(semTelefone, realizada)).isEmpty());
    }

    @Test
    void deveGerarLinkDeContatoComNumeroDaClinica() {
        ConfiguracaoClinicaEntity clinica = new ConfiguracaoClinicaEntity();
        clinica.setTelefoneWhatsapp("65999990000");

        assertEquals("https://wa.me/5565999990000?text=Ol%C3%A1%21%20Sou%20Maria%20Souza%2C%20paciente%20da%20cl%C3%ADnica.",
                service.linkContatoClinica(clinica, paciente(null)));

        clinica.setTelefoneWhatsapp(null);
        assertNull(service.linkContatoClinica(clinica, paciente(null)));
    }

    private ClienteEntity paciente(String telefone) {
        ClienteEntity paciente = new ClienteEntity();
        paciente.setId(UUID.randomUUID());
        paciente.setNomeCompleto("Maria Souza");
        paciente.setTelefone(telefone);
        paciente.setPortalToken(UUID.randomUUID());
        return paciente;
    }

    private SessaoEntity sessao(ClienteEntity paciente, LocalDate data, SessaoStatus status) {
        SessaoEntity sessao = new SessaoEntity();
        sessao.setId(UUID.randomUUID());
        sessao.setCliente(paciente);
        sessao.setStatus(status);
        sessao.setDataHoraInicio(data.atTime(LocalTime.of(9, 30)).atZone(ClinicTimezoneHolder.zoneId()).toOffsetDateTime());
        return sessao;
    }

    private String texto(String url) {
        return URLDecoder.decode(url.substring(url.indexOf("?text=") + 6), StandardCharsets.UTF_8);
    }
}
