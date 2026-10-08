package com.fisiogestao.cadastro;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.auth.InMemoryUserCatalog;
import com.fisiogestao.auth.PasswordPolicy;
import com.fisiogestao.auth.UserRole;
import com.fisiogestao.empresa.ConfiguracaoPlataformaService;
import com.fisiogestao.empresa.EmpresaDadosService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaProvisionamentoService;
import com.fisiogestao.empresa.EmpresaRepository;
import com.fisiogestao.empresa.PeriodoTesteService;
import com.fisiogestao.gestor.EmpresaGestaoService;
import com.fisiogestao.gestor.NotificacaoAdminService;
import com.fisiogestao.gestor.TipoNotificacaoGestor;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cadastro público de uma nova clínica: cria a empresa em TESTE (prazo padrão configurado
 * pelo superadmin, em dias úteis), o primeiro ADMIN_CLINICA e a configuração inicial, libera
 * o acesso imediatamente, audita e avisa o Gestor. Nunca concede acesso à área /gestor.
 */
@Service
@RequiredArgsConstructor
public class CadastroClinicaService {

    /** Mensagem única para duplicidades: não revela se o CNPJ/e-mail já existe em outra clínica. */
    public static final String MENSAGEM_NAO_CONCLUIDO =
            "Não foi possível concluir o cadastro com os dados informados. Verifique os dados ou solicite suporte.";

    private static final Logger LOGGER = LoggerFactory.getLogger(CadastroClinicaService.class);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmpresaDadosService dadosService;
    private final EmpresaRepository empresaRepository;
    private final EmpresaProvisionamentoService provisionamentoService;
    private final PeriodoTesteService periodoTesteService;
    private final ConfiguracaoPlataformaService configuracaoPlataformaService;
    private final InMemoryUserCatalog userCatalog;
    private final AuditService auditService;
    private final NotificacaoAdminService notificacaoAdminService;

    public ResultadoCadastro cadastrar(CadastroClinicaForm form, String ip) {
        String nome = dadosService.normalizarNome(form.getNomeClinica(), "Informe o nome da clínica.");
        String responsavel = dadosService.normalizarNome(form.getNomeResponsavel(), "Informe o nome do responsável.");
        String cpf = dadosService.normalizarCpf(form.getCpf());
        String cnpj = dadosService.normalizarCnpj(form.getCnpj());
        dadosService.exigirCpfOuCnpj(cpf, cnpj);
        String email = dadosService.normalizarEmail(form.getEmail(), true);
        String telefone = dadosService.normalizarTelefone(form.getTelefone(), true);
        if (!form.isAceiteTermos()) {
            throw new BusinessException("É necessário aceitar os termos de uso e a política de privacidade.");
        }
        String senha = PasswordPolicy.normalize(form.getSenha(), true);
        if (!senha.equals(form.getConfirmacaoSenha() == null ? null : form.getConfirmacaoSenha().trim())) {
            throw new BusinessException("A confirmação da senha não confere.");
        }
        if (cnpj != null && empresaRepository.existsByCnpj(cnpj)) {
            LOGGER.info("Cadastro público recusado por duplicidade de CNPJ (ip={}).", ip);
            throw new BusinessException(MENSAGEM_NAO_CONCLUIDO);
        }

        int diasTeste = configuracaoPlataformaService.diasUteisTestePadrao();
        EmpresaEntity nova = EmpresaEntity.builder()
                .nome(nome)
                .slug(dadosService.gerarSlugUnico(nome))
                .nomeResponsavel(responsavel)
                .cpf(cpf)
                .cnpj(cnpj)
                .email(email)
                .telefone(telefone)
                .build();
        periodoTesteService.iniciarTeste(nova, diasTeste, PeriodoTesteService.hoje());

        EmpresaEntity empresa;
        try {
            empresa = provisionamentoService.provisionar(nova, criada -> userCatalog.criarNaEmpresa(
                    criada.getId(), responsavel, usernameInicial(email), email, senha, UserRole.ADMIN_CLINICA,
                    "Primeiro administrador criado pelo cadastro público da clínica."));
        } catch (DataIntegrityViolationException ex) {
            LOGGER.info("Cadastro público recusado por violação de unicidade (ip={}).", ip);
            throw new BusinessException(MENSAGEM_NAO_CONCLUIDO);
        }

        String observacoes = form.getObservacoes() == null || form.getObservacoes().isBlank() ? null : form.getObservacoes().trim();
        Map<String, Object> dadosAuditoria = new LinkedHashMap<>(EmpresaGestaoService.snapshot(empresa));
        dadosAuditoria.put("aceiteTermos", true);
        if (observacoes != null) {
            dadosAuditoria.put("observacoes", observacoes);
        }
        TenantContext.executarSemEmpresa(() -> auditService.recordManual(AuditModule.GESTOR, "empresa",
                empresa.getId().toString(), empresa.getNome(), AuditActionType.CREATE, null, dadosAuditoria,
                null, "Clínica criada pelo cadastro público (status TESTE).", AuditEventOrigin.ONLINE));
        TenantContext.executarComo(empresa.getId(), () -> auditService.recordManual(AuditModule.CONFIGURACOES, "empresa",
                empresa.getId().toString(), empresa.getNome(), AuditActionType.CREATE, null,
                Map.of("status", empresa.getStatus().name(), "diasUteisTeste", diasTeste),
                null, "Cadastro da clínica concluído; período de teste iniciado.", AuditEventOrigin.ONLINE));

        String fimTeste = DATA.format(empresa.getDataFimTeste().atZoneSameInstant(PeriodoTesteService.zonaPlataforma()));
        Map<String, String> detalhes = new LinkedHashMap<>();
        detalhes.put("Clínica", empresa.getNome());
        detalhes.put("Código de acesso", empresa.getSlug());
        detalhes.put("Responsável", responsavel);
        detalhes.put("E-mail", email);
        detalhes.put("Telefone", telefone);
        detalhes.put("Teste até", fimTeste + " (" + diasTeste + " dias úteis)");
        if (observacoes != null) {
            detalhes.put("Observações", observacoes);
        }
        notificacaoAdminService.notificar(TipoNotificacaoGestor.NOVO_CADASTRO,
                "Nova clínica cadastrada: " + empresa.getNome(),
                "Uma nova clínica concluiu o cadastro público e iniciou o período de teste.",
                empresa.getId(), empresa.getId(), "/gestor/empresas/" + empresa.getId(), detalhes);
        notificacaoAdminService.enviarEmailClinica(empresa, "Bem-vindo ao FisioGestao",
                "Seu cadastro foi concluído. Código da clínica: " + empresa.getSlug() + ". Usuário: "
                        + usernameInicial(email) + " (ou o seu e-mail). Período de teste até " + fimTeste + ".", true);

        return new ResultadoCadastro(empresa.getNome(), empresa.getSlug(), usernameInicial(email), email, fimTeste, diasTeste);
    }

    /** O primeiro usuário entra com o próprio e-mail; o username é derivado dele. */
    static String usernameInicial(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-z0-9._-]", "");
        if (base.isBlank()) {
            base = "admin";
        }
        return base.length() > 40 ? base.substring(0, 40) : base;
    }

    public record ResultadoCadastro(String nomeClinica, String slug, String username, String email,
                                    String fimTeste, int diasTeste) {
    }
}
