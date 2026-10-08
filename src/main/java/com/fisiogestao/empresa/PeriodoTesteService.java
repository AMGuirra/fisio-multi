package com.fisiogestao.empresa;

import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Regras do período de teste, sempre em dias úteis ({@link DiasUteisService}). Telas, jobs e
 * cadastro público usam apenas este serviço para calcular início, fim e dias restantes.
 *
 * <p>O teste termina às 23:59:59 do último dia útil, no fuso padrão da plataforma.</p>
 */
@Service
@RequiredArgsConstructor
public class PeriodoTesteService {

    private final DiasUteisService diasUteisService;

    public static ZoneId zonaPlataforma() {
        return ZoneId.of(ClinicTimezoneHolder.DEFAULT_TIMEZONE_ID);
    }

    public static LocalDate hoje() {
        return LocalDate.now(zonaPlataforma());
    }

    /** Coloca a empresa em TESTE com {@code dias} dias úteis contados a partir de {@code inicio}. */
    public void iniciarTeste(EmpresaEntity empresa, int dias, LocalDate inicio) {
        validarDias(dias);
        LocalDate dataInicio = inicio == null ? hoje() : inicio;
        LocalDate dataFim = diasUteisService.calcularDataFinal(dataInicio, dias);
        empresa.setStatus(EmpresaStatus.TESTE);
        empresa.setMotivoBloqueio(null);
        empresa.setDataInicioTeste(dataInicio.atStartOfDay(zonaPlataforma()).toOffsetDateTime());
        empresa.setDataFimTeste(fimDoDia(dataFim));
        empresa.setQuantidadeDiasUteisTeste(dias);
        limparAlertas(empresa);
    }

    /**
     * Altera a quantidade total de dias úteis do teste mantendo a data de início (prorrogar quando
     * maior, reduzir quando menor) e recalcula a data final.
     */
    public void alterarQuantidadeDias(EmpresaEntity empresa, int novaQuantidade) {
        validarDias(novaQuantidade);
        LocalDate inicio = empresa.getDataInicioTeste() == null
                ? hoje()
                : empresa.getDataInicioTeste().atZoneSameInstant(zonaPlataforma()).toLocalDate();
        iniciarTeste(empresa, novaQuantidade, inicio);
    }

    /** Soma {@code diasAdicionais} dias úteis ao teste atual. */
    public void prorrogar(EmpresaEntity empresa, int diasAdicionais) {
        if (diasAdicionais < 1) {
            throw new BusinessException("Informe ao menos 1 dia útil para prorrogar o teste.");
        }
        int atual = empresa.getQuantidadeDiasUteisTeste() == null ? 0 : empresa.getQuantidadeDiasUteisTeste();
        alterarQuantidadeDias(empresa, atual + diasAdicionais);
    }

    /** Define manualmente a data final; a quantidade de dias úteis é recalculada a partir dela. */
    public void definirDataFinal(EmpresaEntity empresa, LocalDate novaDataFinal) {
        if (novaDataFinal == null) {
            throw new BusinessException("Informe a nova data final do teste.");
        }
        LocalDate inicio = empresa.getDataInicioTeste() == null
                ? hoje()
                : empresa.getDataInicioTeste().atZoneSameInstant(zonaPlataforma()).toLocalDate();
        if (novaDataFinal.isBefore(inicio)) {
            throw new BusinessException("A data final do teste não pode ser anterior à data de início.");
        }
        int dias = diasUteisService.contarDiasUteis(inicio, novaDataFinal);
        if (dias < 1) {
            throw new BusinessException("O período informado não contém nenhum dia útil.");
        }
        validarDias(dias);
        empresa.setStatus(EmpresaStatus.TESTE);
        empresa.setMotivoBloqueio(null);
        empresa.setDataInicioTeste(inicio.atStartOfDay(zonaPlataforma()).toOffsetDateTime());
        empresa.setDataFimTeste(fimDoDia(novaDataFinal));
        empresa.setQuantidadeDiasUteisTeste(dias);
        limparAlertas(empresa);
    }

    /** Dias úteis restantes depois de hoje (0 no último dia; negativo não ocorre). */
    public int diasUteisRestantes(EmpresaEntity empresa) {
        if (empresa.getDataFimTeste() == null) {
            return 0;
        }
        LocalDate fim = empresa.getDataFimTeste().atZoneSameInstant(zonaPlataforma()).toLocalDate();
        return diasUteisService.diasUteisRestantes(hoje(), fim);
    }

    public boolean testeExpirado(EmpresaEntity empresa, OffsetDateTime agora) {
        return empresa.getStatus() == EmpresaStatus.TESTE
                && (empresa.getDataFimTeste() == null || agora.isAfter(empresa.getDataFimTeste()));
    }

    private static OffsetDateTime fimDoDia(LocalDate data) {
        return data.atTime(LocalTime.of(23, 59, 59)).atZone(zonaPlataforma()).toOffsetDateTime();
    }

    private static void limparAlertas(EmpresaEntity empresa) {
        empresa.setAlertaVencimentoTesteEm(null);
        empresa.setAlertaExpiracaoTesteEm(null);
    }

    private static void validarDias(int dias) {
        if (dias < 1 || dias > ConfiguracaoPlataformaService.MAX_DIAS_UTEIS_TESTE) {
            throw new BusinessException("A quantidade de dias úteis deve estar entre 1 e "
                    + ConfiguracaoPlataformaService.MAX_DIAS_UTEIS_TESTE + ".");
        }
    }
}
