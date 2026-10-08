package com.fisiogestao.empresa;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Único ponto de cálculo de dias úteis da plataforma (período de teste, avisos de vencimento).
 *
 * <p>Não são dias úteis: sábados, domingos e feriados nacionais brasileiros — Confraternização
 * Universal (01/01), Sexta-feira Santa (móvel), Tiradentes (21/04), Dia do Trabalho (01/05),
 * Independência (07/09), Nossa Senhora Aparecida (12/10), Finados (02/11), Proclamação da
 * República (15/11), Dia Nacional de Zumbi e da Consciência Negra (20/11, nacional desde 2024,
 * Lei 14.759/2023) e Natal (25/12). Carnaval e Corpus Christi são pontos facultativos federais
 * e por isso não são descontados.</p>
 */
@Service
public class DiasUteisService {

    private static final Set<MonthDay> FERIADOS_FIXOS = Set.of(
            MonthDay.of(1, 1),
            MonthDay.of(4, 21),
            MonthDay.of(5, 1),
            MonthDay.of(9, 7),
            MonthDay.of(10, 12),
            MonthDay.of(11, 2),
            MonthDay.of(11, 15),
            MonthDay.of(12, 25)
    );

    private static final int ANO_INICIO_CONSCIENCIA_NEGRA = 2024;

    private final Map<Integer, Set<LocalDate>> cachePorAno = new ConcurrentHashMap<>();

    public boolean isDiaUtil(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            return false;
        }
        return !feriadosNacionais(data.getYear()).contains(data);
    }

    public boolean isFeriadoNacional(LocalDate data) {
        return feriadosNacionais(data.getYear()).contains(data);
    }

    /**
     * Último dia do período de teste com {@code quantidade} dias úteis começando em {@code inicio}.
     * O próprio dia de início conta quando for útil; se não for, a contagem começa no próximo dia útil.
     */
    public LocalDate calcularDataFinal(LocalDate inicio, int quantidade) {
        if (inicio == null) {
            throw new IllegalArgumentException("Data de início obrigatória.");
        }
        if (quantidade < 1) {
            throw new IllegalArgumentException("A quantidade de dias úteis deve ser maior que zero.");
        }
        LocalDate data = inicio;
        int contados = 0;
        while (true) {
            if (isDiaUtil(data)) {
                contados++;
                if (contados == quantidade) {
                    return data;
                }
            }
            data = data.plusDays(1);
        }
    }

    /** Quantidade de dias úteis no intervalo fechado [inicio, fim]; zero quando fim &lt; inicio. */
    public int contarDiasUteis(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            return 0;
        }
        int total = 0;
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            if (isDiaUtil(data)) {
                total++;
            }
        }
        return total;
    }

    /** Dias úteis restantes depois de {@code hoje} até {@code fim} (hoje não entra na conta). */
    public int diasUteisRestantes(LocalDate hoje, LocalDate fim) {
        return contarDiasUteis(hoje.plusDays(1), fim);
    }

    public Set<LocalDate> feriadosNacionais(int ano) {
        return cachePorAno.computeIfAbsent(ano, DiasUteisService::calcularFeriados);
    }

    private static Set<LocalDate> calcularFeriados(int ano) {
        Set<LocalDate> feriados = new TreeSet<>();
        FERIADOS_FIXOS.forEach(dia -> feriados.add(dia.atYear(ano)));
        if (ano >= ANO_INICIO_CONSCIENCIA_NEGRA) {
            feriados.add(LocalDate.of(ano, 11, 20));
        }
        feriados.add(domingoDePascoa(ano).minusDays(2));
        return Set.copyOf(feriados);
    }

    /** Algoritmo de Meeus/Jones/Butcher para o calendário gregoriano. */
    static LocalDate domingoDePascoa(int ano) {
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }
}
