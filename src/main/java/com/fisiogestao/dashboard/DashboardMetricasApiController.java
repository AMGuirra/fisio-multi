package com.fisiogestao.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de métricas históricas do dashboard.
 *
 * <p>Retorna os dados necessários para renderização de gráficos evolutivos
 * no frontend: ocupação mensal, performance financeira e retenção de pacotes.
 * Todos os agregados são computados via SQL nativo no PostgreSQL.</p>
 *
 * <p><strong>Acesso:</strong> ADMIN sempre tem acesso; para as demais roles, depende
 * do menu "Financeiro" estar liberado em "Funções de Usuários" (ver
 * {@code MenuAccessSecurity} e {@code UserMenuAccessService}). A autenticação é
 * baseada em sessão (form-login); HTTP Basic está desabilitado nesta aplicação.</p>
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Métricas históricas para gráficos gerenciais — requer acesso ao menu Financeiro")
public class DashboardMetricasApiController {

    private final DashboardMetricasService metricasService;

    @GetMapping("/metricas")
    @Operation(
            summary = "Métricas históricas dos últimos 12 meses",
            description = """
                    Retorna quatro conjuntos de dados para renderização de gráficos:

                    - **Ocupação mensal**: total de horas agendadas por mês (sessões não canceladas).
                    - **Receita mensal**: receita recebida segregada entre sessões avulsas e pacotes de tratamento.
                    - **Retenção de pacotes**: taxa de conversão de pacientes para planos e taxa de renovação.
                    - **Frequência de pacientes**: ranking dos pacientes que mais realizaram sessões.

                    Todas as queries operam diretamente no PostgreSQL via agregados nativos.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Métricas carregadas com sucesso",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = DashboardMetricasService.MetricasDashboard.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Não autenticado"),
                    @ApiResponse(responseCode = "403", description = "Sem permissão")
            }
    )
    public ResponseEntity<DashboardMetricasService.MetricasDashboard> metricas() {
        return ResponseEntity.ok(metricasService.carregarMetricas());
    }
}
