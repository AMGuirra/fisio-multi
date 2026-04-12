package com.fisiogestao.status;

import com.fisiogestao.config.ClinicTimezoneHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemStatusService {

    private final SystemStatusRepository repository;
    private volatile SystemStatusEntity cachedStatus;

    public SystemStatusEntity getStatusAtual() {
        SystemStatusEntity localCache = cachedStatus;
        if (localCache != null) {
            return localCache;
        }

        synchronized (this) {
            if (cachedStatus != null) {
                return cachedStatus;
            }

            cachedStatus = repository.findFirstByOrderByAtualizadoEmDesc()
                    .orElseGet(() -> repository.save(new SystemStatusEntity(
                            SystemStatusType.ONLINE,
                            "Sistema operando normalmente"
                    )));
            return cachedStatus;
        }
    }

    @Transactional
    public SystemStatusEntity atualizarStatus(SystemStatusType novoStatus, String mensagem) {
        SystemStatusEntity status = new SystemStatusEntity();
        status.setStatus(novoStatus);
        status.setMensagem(mensagem);
        status.setAtualizadoEm(ClinicTimezoneHolder.now().toLocalDateTime());
        SystemStatusEntity salvo = repository.save(status);
        cachedStatus = salvo;
        return salvo;
    }
}
