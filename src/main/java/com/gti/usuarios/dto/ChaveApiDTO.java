package com.gti.usuarios.dto;

import com.gti.usuarios.model.ChaveApi;

import java.time.LocalDateTime;
import java.util.List;

public record ChaveApiDTO(
        Long id,
        String nome,
        String prefixo,
        List<String> escopos,
        List<String> ipsPermitidos,
        LocalDateTime expiraEm,
        String situacao,
        String criadoPorNome,
        LocalDateTime criadoEm,
        LocalDateTime desligadaEm,
        String desligadaPorNome,
        LocalDateTime ultimoUsoEm,
        String ultimoUsoIp
) {
    public static ChaveApiDTO from(ChaveApi c) {
        return new ChaveApiDTO(
                c.getId(),
                c.getNome(),
                c.getPrefixo(),
                c.getEscoposLista(),
                c.getIpsLista(),
                c.getExpiraEm(),
                situacaoDe(c),
                c.getCriadoPorNome(),
                c.getCriadoEm(),
                c.getDesligadaEm(),
                c.getDesligadaPorNome(),
                c.getUltimoUsoEm(),
                c.getUltimoUsoIp()
        );
    }

    private static String situacaoDe(ChaveApi c) {
        if (!c.isAtiva()) return "DESLIGADA";
        if (c.isExpirada()) return "EXPIRADA";
        return "ATIVA";
    }
}