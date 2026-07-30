package com.gti.usuarios.audit;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Map;

public class AuditoriaListener {

    private static final Logger log = LoggerFactory.getLogger("com.gti.auditoria");

    private static final Map<String, String> NOMES = Map.of(
            "Usuario",        "usuário",
            "TipoAtivo",      "tipo de ativo",
            "CampoDinamico",  "campo dinâmico",
            "Ativo",          "ativo",
            "ValorCampo",     "valor de campo",
            "AnexoAtivo",     "anexo",
            "HistoricoAtivo", "registro de histórico",
            "Manutencao",     "manutenção",
            "Chamado",        "chamado",
            "MensagemChamado","mensagem de chamado"
    );

    @PostPersist
    public void aoCriar(Object e) { registrar("CRIOU", e); }

    @PostUpdate
    public void aoEditar(Object e) { registrar("EDITOU", e); }

    @PostRemove
    public void aoExcluir(Object e) { registrar("EXCLUIU", e); }

    private void registrar(String acao, Object e) {
        String tipo = e.getClass().getSimpleName();
        log.info("{} {} {} [id={}]{}",
                usuarioAtual(),
                acao,
                NOMES.getOrDefault(tipo, tipo.toLowerCase()),
                valor(e, "getId"),
                descricao(e));
    }

    private String usuarioAtual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getName() == null || "anonymousUser".equals(a.getName()))
            return "[sistema]";
        return "[" + a.getName() + "]";
    }

    private String descricao(Object e) {
        for (String getter : new String[]{"getNomeUsuario", "getPatrimonio", "getNome", "getTitulo"}) {
            Object v = valor(e, getter);
            if (v != null) return " " + v;
        }
        return "";
    }

    private Object valor(Object e, String getter) {
        try {
            Method m = e.getClass().getMethod(getter);
            return m.invoke(e);
        } catch (Exception ex) {
            return null;
        }
    }
}