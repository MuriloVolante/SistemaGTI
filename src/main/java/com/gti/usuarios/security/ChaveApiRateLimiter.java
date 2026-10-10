package com.gti.usuarios.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChaveApiRateLimiter {

    private static class Janela {
        int chamadas;
        Instant inicio;
    }

    @Value("${integracao.limite-por-minuto:120}")
    private int limitePorMinuto;

    private final Map<Long, Janela> janelas = new ConcurrentHashMap<>();

    public boolean permitir(Long chaveId) {
        if (limitePorMinuto <= 0) return true;
        Instant agora = Instant.now();
        Janela j = janelas.computeIfAbsent(chaveId, k -> {
            Janela nova = new Janela();
            nova.inicio = agora;
            return nova;
        });
        synchronized (j) {
            if (j.inicio.plusSeconds(60).isBefore(agora)) {
                j.inicio = agora;
                j.chamadas = 0;
            }
            if (j.chamadas >= limitePorMinuto) return false;
            j.chamadas++;
            return true;
        }
    }

    public int getLimitePorMinuto() {
        return limitePorMinuto;
    }

    public void limparAntigas() {
        Instant corte = Instant.now().minusSeconds(300);
        janelas.entrySet().removeIf(e -> e.getValue().inicio.isBefore(corte));
    }
}