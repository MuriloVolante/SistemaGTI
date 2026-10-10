package com.gti.usuarios.dto;

import com.gti.usuarios.model.ChaveApiUso;

import java.time.LocalDateTime;

public record ChaveApiUsoDTO(
        Long id,
        Long chaveId,
        String chaveNome,
        String prefixoTentado,
        String endpoint,
        String metodo,
        String ip,
        Integer statusHttp,
        boolean sucesso,
        String motivoNegacao,
        LocalDateTime ocorridoEm
) {
    public static ChaveApiUsoDTO from(ChaveApiUso u) {
        return new ChaveApiUsoDTO(
                u.getId(), u.getChaveId(), u.getChaveNome(), u.getPrefixoTentado(),
                u.getEndpoint(), u.getMetodo(), u.getIp(), u.getStatusHttp(),
                u.isSucesso(), u.getMotivoNegacao(), u.getOcorridoEm()
        );
    }
}