package com.fisiogestao.portal;

import com.fisiogestao.agenda.SessaoEntity;

/**
 * Visão de uma sessão agendada no portal do paciente.
 * Agrega a entidade com dados pré-computados para o template,
 * evitando lógica complexa no Thymeleaf.
 *
 * @param sessao      entidade da sessão
 * @param calendarUrl URL pronta para adicionar ao Google Calendar
 */
public record SessaoPortalView(SessaoEntity sessao, String calendarUrl) {
}
