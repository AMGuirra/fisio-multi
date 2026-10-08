package com.fisiogestao.gestor;

import com.fisiogestao.cancelamento.CancelamentoService;
import com.fisiogestao.empresa.ConfiguracaoPlataformaService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaRepository;
import com.fisiogestao.empresa.EmpresaStatus;
import com.fisiogestao.empresa.PeriodoTesteService;
import com.fisiogestao.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rotinas diárias da plataforma: avisa testes próximos do fim (N dias úteis antes, configurável),
 * avisa testes expirados (uma vez por período) e conclui cancelamentos agendados.
 * A negação de acesso de testes expirados não depende deste job: é calculada a cada requisição.
 */
@Service
@RequiredArgsConstructor
public class PlataformaAgendamentosService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlataformaAgendamentosService.class);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmpresaRepository empresaRepository;
    private final PeriodoTesteService periodoTesteService;
    private final ConfiguracaoPlataformaService configuracaoPlataformaService;
    private final NotificacaoAdminService notificacaoAdminService;
    private final CancelamentoService cancelamentoService;

    @Scheduled(cron = "${app.jobs.plataforma-cron:0 0 8 * * *}", zone = "America/Campo_Grande")
    public void executarRotinaDiaria() {
        TenantContext.executarSemEmpresa(() -> {
            try {
                verificarTestes(OffsetDateTime.now());
            } catch (RuntimeException ex) {
                LOGGER.error("Falha ao verificar períodos de teste: {}", ex.getMessage(), ex);
            }
            try {
                int concluidos = cancelamentoService.concluirAgendadosVencidos(PeriodoTesteService.hoje());
                if (concluidos > 0) {
                    LOGGER.info("{} cancelamento(s) agendado(s) concluído(s).", concluidos);
                }
            } catch (RuntimeException ex) {
                LOGGER.error("Falha ao concluir cancelamentos agendados: {}", ex.getMessage(), ex);
            }
        });
    }

    /** Envia, no máximo uma vez por período de teste, o aviso de vencimento próximo e o de expiração. */
    @Transactional
    public void verificarTestes(OffsetDateTime agora) {
        int diasAviso = configuracaoPlataformaService.diasUteisAvisoVencimento();
        for (EmpresaEntity empresa : empresaRepository.findByStatus(EmpresaStatus.TESTE)) {
            String fim = empresa.getDataFimTeste() == null ? "-"
                    : DATA.format(empresa.getDataFimTeste().atZoneSameInstant(PeriodoTesteService.zonaPlataforma()));
            Map<String, String> detalhes = new LinkedHashMap<>();
            detalhes.put("Clínica", empresa.getNome());
            detalhes.put("Código", empresa.getSlug());
            detalhes.put("Fim do teste", fim);

            if (periodoTesteService.testeExpirado(empresa, agora)) {
                if (empresa.getAlertaExpiracaoTesteEm() == null) {
                    notificacaoAdminService.notificar(TipoNotificacaoGestor.TESTE_EXPIRADO,
                            "Teste expirado: " + empresa.getNome(),
                            "O período de teste terminou e o acesso da clínica foi suspenso. Os dados estão preservados.",
                            empresa.getId(), empresa.getId(), "/gestor/empresas/" + empresa.getId(), detalhes);
                    notificacaoAdminService.enviarEmailClinica(empresa, "Período de teste finalizado",
                            "O período de teste da sua clínica foi finalizado. Entre em contato para continuar usando o sistema.", false);
                    empresa.setAlertaExpiracaoTesteEm(agora);
                    empresaRepository.save(empresa);
                }
                continue;
            }

            int restantes = periodoTesteService.diasUteisRestantes(empresa);
            if (restantes <= diasAviso && empresa.getAlertaVencimentoTesteEm() == null) {
                detalhes.put("Dias úteis restantes", String.valueOf(restantes));
                notificacaoAdminService.notificar(TipoNotificacaoGestor.TESTE_VENCENDO,
                        "Teste próximo do fim: " + empresa.getNome(),
                        "O período de teste termina em " + restantes + " dia(s) útil(eis).",
                        empresa.getId(), empresa.getId(), "/gestor/empresas/" + empresa.getId(), detalhes);
                notificacaoAdminService.enviarEmailClinica(empresa, "Seu período de teste está terminando",
                        "O período de teste da sua clínica termina em " + fim + ".", true);
                empresa.setAlertaVencimentoTesteEm(agora);
                empresaRepository.save(empresa);
            }
        }
    }
}
