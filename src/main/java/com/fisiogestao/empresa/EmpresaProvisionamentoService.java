package com.fisiogestao.empresa;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalTime;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Cria uma empresa e seus dados iniciais. A empresa é gravada primeiro (fora de tenant);
 * em seguida, numa transação aberta já no escopo da nova empresa, são criados os dados
 * da clínica (configuração e, opcionalmente, o primeiro usuário). Se a segunda etapa falhar,
 * a empresa recém-criada é removida para não deixar cadastro pela metade.
 */
@Service
@RequiredArgsConstructor
public class EmpresaProvisionamentoService {

    private final EmpresaRepository empresaRepository;
    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;
    private final PlatformTransactionManager transactionManager;

    public EmpresaEntity provisionar(EmpresaEntity nova, Consumer<EmpresaEntity> dadosIniciaisDaClinica) {
        TransactionTemplate novaTransacao = new TransactionTemplate(transactionManager);
        novaTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        EmpresaEntity salva = TenantContext.executarSemEmpresa(() -> novaTransacao.execute(status -> empresaRepository.save(nova)));
        UUID empresaId = salva.getId();
        try {
            TenantContext.executarComo(empresaId, () -> novaTransacao.executeWithoutResult(status -> {
                criarConfiguracaoInicial(salva);
                if (dadosIniciaisDaClinica != null) {
                    dadosIniciaisDaClinica.accept(salva);
                }
            }));
        } catch (RuntimeException ex) {
            TenantContext.executarSemEmpresa(() -> novaTransacao.executeWithoutResult(status -> empresaRepository.deleteById(empresaId)));
            throw ex;
        }
        return salva;
    }

    /** Garante a configuração operacional da clínica do contexto atual (idempotente). */
    public void criarConfiguracaoInicial(EmpresaEntity empresa) {
        if (configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc().isPresent()) {
            return;
        }
        ConfiguracaoClinicaEntity config = ConfiguracaoClinicaEntity.builder()
                .nomeClinica(empresa.getNome())
                .timezoneId(ClinicTimezoneHolder.DEFAULT_TIMEZONE_ID)
                .horarioAbertura(LocalTime.of(7, 0))
                .horarioFechamento(LocalTime.of(19, 0))
                .duracaoSessaoMinutos(60)
                .horarioInicioAlmoco(LocalTime.of(12, 0))
                .horarioFimAlmoco(LocalTime.of(13, 0))
                .diasFuncionamento("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                .telefoneWhatsapp(empresa.getTelefone())
                .emailContato(empresa.getEmail())
                .ativo(true)
                .build();
        configuracaoClinicaRepository.save(config);
        ClinicTimezoneHolder.registrar(empresa.getId(), config.getTimezoneId());
    }
}
