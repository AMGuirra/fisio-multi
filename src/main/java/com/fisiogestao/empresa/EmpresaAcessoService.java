package com.fisiogestao.empresa;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Regra única de licença/acesso da clínica, usada no login, a cada requisição autenticada,
 * nas rotas públicas e nos jobs agendados.
 */
@Service
@RequiredArgsConstructor
public class EmpresaAcessoService {

    private final EmpresaRepository empresaRepository;

    /** Vazio quando o acesso está liberado; caso contrário, o motivo da negação. */
    public static Optional<MotivoAcessoNegado> avaliar(EmpresaEntity empresa, OffsetDateTime agora) {
        if (empresa == null || empresa.getStatus() == null) {
            return Optional.of(MotivoAcessoNegado.INATIVO);
        }
        return switch (empresa.getStatus()) {
            case ATIVO -> Optional.empty();
            case BLOQUEADO -> Optional.of(MotivoAcessoNegado.BLOQUEADO);
            case INATIVO -> Optional.of(MotivoAcessoNegado.INATIVO);
            case TESTE -> empresa.getDataFimTeste() != null && !agora.isAfter(empresa.getDataFimTeste())
                    ? Optional.empty()
                    : Optional.of(MotivoAcessoNegado.TESTE_EXPIRADO);
        };
    }

    @Transactional(readOnly = true)
    public Optional<MotivoAcessoNegado> avaliar(UUID empresaId) {
        if (empresaId == null) {
            return Optional.of(MotivoAcessoNegado.INATIVO);
        }
        return avaliar(empresaRepository.findById(empresaId).orElse(null), OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public boolean acessoLiberado(UUID empresaId) {
        return avaliar(empresaId).isEmpty();
    }
}
