package com.fisiogestao.agenda;

import java.time.DayOfWeek;

public enum DiaSemana {
    SEG(DayOfWeek.MONDAY),
    TER(DayOfWeek.TUESDAY),
    QUA(DayOfWeek.WEDNESDAY),
    QUI(DayOfWeek.THURSDAY),
    SEX(DayOfWeek.FRIDAY),
    SAB(DayOfWeek.SATURDAY),
    DOM(DayOfWeek.SUNDAY);

    private final DayOfWeek dayOfWeek;

    DiaSemana(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public DayOfWeek toDayOfWeek() {
        return dayOfWeek;
    }

    public static DiaSemana from(DayOfWeek dayOfWeek) {
        for (DiaSemana dia : values()) {
            if (dia.dayOfWeek == dayOfWeek) {
                return dia;
            }
        }
        throw new IllegalArgumentException("Dia da semana inválido: " + dayOfWeek);
    }
}
