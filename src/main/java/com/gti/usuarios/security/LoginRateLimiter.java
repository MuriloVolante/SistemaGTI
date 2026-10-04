package com.gti.usuarios.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_TENTATIVAS = 5;
    private static final long JANELA_SEGUNDOS = 15 * 60;
    private static final long BLOQUEIO_SEGUNDOS = 15 * 60;

    private static class Registro {
        int tentativas;
        Instant primeiraTentativa;
        Instant bloqueadoAte;
    }

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    private String chave(String nomeUsuario) {
        return nomeUsuario == null ? "" : nomeUsuario.trim().toLowerCase();
    }

    public long segundosRestantes(String nomeUsuario) {
        String k = chave(nomeUsuario);
        Registro r = registros.get(k);
        if (r == null || r.bloqueadoAte == null) return 0;
        long restante = r.bloqueadoAte.getEpochSecond() - Instant.now().getEpochSecond();
        if (restante <= 0) {
            registros.remove(k);
            return 0;
        }
        return restante;
    }

    public synchronized void registrarFalha(String nomeUsuario) {
        String k = chave(nomeUsuario);
        Instant agora = Instant.now();
        Registro r = registros.computeIfAbsent(k, x -> new Registro());

        if (r.primeiraTentativa == null
                || agora.getEpochSecond() - r.primeiraTentativa.getEpochSecond() > JANELA_SEGUNDOS) {
            r.primeiraTentativa = agora;
            r.tentativas = 0;
        }

        r.tentativas++;
        if (r.tentativas >= MAX_TENTATIVAS) {
            r.bloqueadoAte = agora.plusSeconds(BLOQUEIO_SEGUNDOS);
            r.tentativas = 0;
            r.primeiraTentativa = null;
        }
    }

    public void registrarSucesso(String nomeUsuario) {
        registros.remove(chave(nomeUsuario));
    }
}