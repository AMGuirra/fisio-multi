package com.fisiogestao.empresa;

import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ConfiguracaoPlataformaService {

    public static final int MAX_DIAS_UTEIS_TESTE = 3650;

    private final ConfiguracaoPlataformaRepository repository;

    /** Retorna a configuração única; cria com os padrões caso o banco ainda não tenha o registro. */
    @Transactional
    public ConfiguracaoPlataformaEntity obter() {
        return repository.findAll().stream()
                .findFirst()
                .orElseGet(() -> repository.save(ConfiguracaoPlataformaEntity.builder()
                        .diasUteisTestePadrao(ConfiguracaoPlataformaEntity.DIAS_UTEIS_TESTE_PADRAO)
                        .diasUteisAvisoVencimento(ConfiguracaoPlataformaEntity.DIAS_UTEIS_AVISO_PADRAO)
                        .build()));
    }

    @Transactional
    public int diasUteisTestePadrao() {
        Integer dias = obter().getDiasUteisTestePadrao();
        return dias == null || dias < 1 ? ConfiguracaoPlataformaEntity.DIAS_UTEIS_TESTE_PADRAO : dias;
    }

    @Transactional
    public int diasUteisAvisoVencimento() {
        Integer dias = obter().getDiasUteisAvisoVencimento();
        return dias == null || dias < 1 ? ConfiguracaoPlataformaEntity.DIAS_UTEIS_AVISO_PADRAO : dias;
    }

    @Transactional
    public String emailNotificacoes() {
        return obter().getEmailNotificacoes();
    }

    @Transactional
    public ConfiguracaoPlataformaEntity salvar(Integer diasTestePadrao, Integer diasAviso, String emailNotificacoes) {
        ConfiguracaoPlataformaEntity config = obter();
        config.setDiasUteisTestePadrao(validarDias(diasTestePadrao, "Informe a quantidade padrão de dias úteis de teste."));
        config.setDiasUteisAvisoVencimento(validarDias(diasAviso, "Informe com quantos dias úteis de antecedência avisar o vencimento."));
        config.setEmailNotificacoes(normalizarEmail(emailNotificacoes));
        return repository.save(config);
    }

    static int validarDias(Integer dias, String mensagemObrigatorio) {
        if (dias == null) {
            throw new BusinessException(mensagemObrigatorio);
        }
        if (dias < 1 || dias > MAX_DIAS_UTEIS_TESTE) {
            throw new BusinessException("A quantidade de dias úteis deve estar entre 1 e " + MAX_DIAS_UTEIS_TESTE + ".");
        }
        return dias;
    }

    private String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalizado = email.trim().toLowerCase(Locale.ROOT);
        if (!normalizado.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || normalizado.length() > 160) {
            throw new BusinessException("Informe um e-mail administrativo válido.");
        }
        return normalizado;
    }
}
