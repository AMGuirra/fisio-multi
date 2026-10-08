package com.fisiogestao.empresa;

import com.fisiogestao.shared.util.DocumentoUtils;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

/** Normalização e validação dos dados cadastrais de empresas (Gestor e cadastro público). */
@Service
@RequiredArgsConstructor
public class EmpresaDadosService {

    private static final int SLUG_MAX = 60;

    private final EmpresaRepository empresaRepository;

    public String normalizarNome(String nome, String mensagem) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessException(mensagem);
        }
        String normalizado = nome.trim().replaceAll("\\s+", " ");
        if (normalizado.length() > 160) {
            throw new BusinessException("O nome deve ter no máximo 160 caracteres.");
        }
        return normalizado;
    }

    public String normalizarNomeOpcional(String nome) {
        return nome == null || nome.isBlank() ? null : normalizarNome(nome, "Nome inválido.");
    }

    /** CPF opcional: vazio vira {@code null}; preenchido precisa ser válido. */
    public String normalizarCpf(String cpf) {
        String digitos = DocumentoUtils.somenteDigitos(cpf);
        if (digitos == null || digitos.isEmpty()) {
            return null;
        }
        if (!DocumentoUtils.cpfValido(digitos)) {
            throw new BusinessException("Informe um CPF válido.");
        }
        return digitos;
    }

    /** CNPJ opcional: vazio vira {@code null}; preenchido precisa ser válido. */
    public String normalizarCnpj(String cnpj) {
        String digitos = DocumentoUtils.somenteDigitos(cnpj);
        if (digitos == null || digitos.isEmpty()) {
            return null;
        }
        if (!DocumentoUtils.cnpjValido(digitos)) {
            throw new BusinessException("Informe um CNPJ válido.");
        }
        return digitos;
    }

    public void exigirCpfOuCnpj(String cpf, String cnpj) {
        if (cpf == null && cnpj == null) {
            throw new BusinessException("Informe o CPF ou o CNPJ.");
        }
    }

    public String normalizarEmail(String email, boolean obrigatorio) {
        if (email == null || email.isBlank()) {
            if (obrigatorio) {
                throw new BusinessException("Informe o e-mail.");
            }
            return null;
        }
        String normalizado = email.trim().toLowerCase(Locale.ROOT);
        if (normalizado.length() > 160 || !normalizado.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException("Informe um e-mail válido.");
        }
        return normalizado;
    }

    public String normalizarTelefone(String telefone, boolean obrigatorio) {
        String digitos = DocumentoUtils.somenteDigitos(telefone);
        if (digitos == null || digitos.isEmpty()) {
            if (obrigatorio) {
                throw new BusinessException("Informe o telefone.");
            }
            return null;
        }
        if (!DocumentoUtils.telefoneTemTamanhoValido(digitos)) {
            throw new BusinessException("Informe um telefone com DDD (10 ou 11 dígitos).");
        }
        return digitos;
    }

    /** Valida um slug informado manualmente (Gestor). */
    public String validarSlug(String slug, UUID idIgnorado) {
        if (slug == null || slug.isBlank()) {
            throw new BusinessException("Informe o código (slug) da clínica.");
        }
        String normalizado = slug.trim().toLowerCase(Locale.ROOT);
        if (!normalizado.matches("^[a-z0-9]+(-[a-z0-9]+)*$") || normalizado.length() > SLUG_MAX) {
            throw new BusinessException("O código deve conter apenas letras minúsculas, números e hífens (até 60 caracteres).");
        }
        boolean emUso = idIgnorado == null
                ? empresaRepository.existsBySlug(normalizado)
                : empresaRepository.existsBySlugAndIdNot(normalizado, idIgnorado);
        if (emUso) {
            throw new BusinessException("Este código já está em uso por outra clínica.");
        }
        return normalizado;
    }

    /** Gera um slug único a partir do nome (sem acentos), acrescentando sufixo numérico se necessário. */
    public String gerarSlugUnico(String nome) {
        String base = Normalizer.normalize(nome == null ? "" : nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (base.isEmpty()) {
            base = "clinica";
        }
        if (base.length() > SLUG_MAX - 5) {
            base = base.substring(0, SLUG_MAX - 5).replaceAll("-+$", "");
        }
        String candidato = base;
        int sufixo = 2;
        while (empresaRepository.existsBySlug(candidato)) {
            candidato = base + "-" + sufixo++;
        }
        return candidato;
    }

    public void garantirCnpjDisponivel(String cnpj, UUID idIgnorado) {
        if (cnpj == null) {
            return;
        }
        boolean emUso = idIgnorado == null
                ? empresaRepository.existsByCnpj(cnpj)
                : empresaRepository.existsByCnpjAndIdNot(cnpj, idIgnorado);
        if (emUso) {
            throw new BusinessException("Já existe uma clínica cadastrada com este CNPJ.");
        }
    }
}
