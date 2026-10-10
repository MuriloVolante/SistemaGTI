package com.gti.usuarios.service;

import com.gti.usuarios.model.ChaveApi;
import com.gti.usuarios.model.ChaveApiUso;
import com.gti.usuarios.repository.ChaveApiRepository;
import com.gti.usuarios.repository.ChaveApiUsoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ChaveApiUsoService {

    private static final Logger log = LoggerFactory.getLogger(ChaveApiUsoService.class);

    private final ChaveApiUsoRepository usoRepo;
    private final ChaveApiRepository chaveRepo;

    public ChaveApiUsoService(ChaveApiUsoRepository usoRepo, ChaveApiRepository chaveRepo) {
        this.usoRepo = usoRepo;
        this.chaveRepo = chaveRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(ChaveApi chave,
                          String prefixoTentado,
                          String endpoint,
                          String metodo,
                          String ip,
                          String userAgent,
                          int statusHttp,
                          boolean sucesso,
                          String motivoNegacao) {
        try {
            ChaveApiUso u = new ChaveApiUso();
            if (chave != null) {
                u.setChaveId(chave.getId());
                u.setChaveNome(chave.getNome());
                u.setPrefixoTentado(chave.getPrefixo());
            } else {
                u.setPrefixoTentado(prefixoTentado);
            }
            u.setEndpoint(cortar(endpoint, 200));
            u.setMetodo(cortar(metodo, 10));
            u.setIp(cortar(ip, 60));
            u.setUserAgent(cortar(userAgent, 255));
            u.setStatusHttp(statusHttp);
            u.setSucesso(sucesso);
            u.setMotivoNegacao(cortar(motivoNegacao, 60));
            u.setOcorridoEm(LocalDateTime.now());
            usoRepo.save(u);

            if (chave != null && sucesso) {
                chave.setUltimoUsoEm(u.getOcorridoEm());
                chave.setUltimoUsoIp(u.getIp());
                chaveRepo.save(chave);
            }
        } catch (Exception e) {
            log.error("Falha ao registrar uso da API de integracao", e);
        }
    }

    private String cortar(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }
}