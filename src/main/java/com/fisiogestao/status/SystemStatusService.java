package com.fisiogestao.status;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SystemStatusService {

    private final SystemStatusRepository repository;

    public SystemStatusEntity getStatusAtual() {
        return repository.findUltimoRegistro()
                .orElseGet(() -> {
                    SystemStatusEntity padrao = new SystemStatusEntity(
                            SystemStatusType.ONLINE,
                            "Sistema operando normalmente"
                    );
                    return repository.save(padrao);
                });
    }

    @Transactional
    public SystemStatusEntity atualizarStatus(SystemStatusType novoStatus, String mensagem) {
        SystemStatusEntity status = new SystemStatusEntity();
        status.setStatus(novoStatus);
        status.setMensagem(mensagem);
        status.setAtualizadoEm(LocalDateTime.now());
        return repository.save(status);
    }
}