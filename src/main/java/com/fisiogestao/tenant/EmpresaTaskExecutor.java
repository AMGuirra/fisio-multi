package com.fisiogestao.tenant;

import com.fisiogestao.empresa.EmpresaAcessoService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * Executa rotinas agendadas separadamente para cada clínica com acesso liberado. Cada
 * execução abre o escopo da empresa antes de iniciar a transação, para que o Hibernate
 * aplique o filtro de tenant correto; uma falha numa clínica não interrompe as demais.
 */
@Component
@RequiredArgsConstructor
public class EmpresaTaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmpresaTaskExecutor.class);

    private final EmpresaRepository empresaRepository;
    private final PlatformTransactionManager transactionManager;

    public void paraCadaEmpresaAtiva(String nomeRotina, Consumer<EmpresaEntity> rotina) {
        List<EmpresaEntity> empresas = TenantContext.executarSemEmpresa(() -> empresaRepository.findAll());
        OffsetDateTime agora = OffsetDateTime.now();
        TransactionTemplate transacao = new TransactionTemplate(transactionManager);
        transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (EmpresaEntity empresa : empresas) {
            if (EmpresaAcessoService.avaliar(empresa, agora).isPresent()) {
                continue;
            }
            try {
                TenantContext.executarComo(empresa.getId(), () ->
                        transacao.executeWithoutResult(status -> rotina.accept(empresa)));
            } catch (RuntimeException ex) {
                LOGGER.error("[{}] Falha na empresa {}: {}", nomeRotina, empresa.getId(), ex.getMessage(), ex);
            }
        }
    }
}
