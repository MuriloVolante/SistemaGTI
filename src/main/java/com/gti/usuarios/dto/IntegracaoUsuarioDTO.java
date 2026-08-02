package com.gti.usuarios.dto;

import com.gti.usuarios.model.Usuario;

import java.time.LocalDateTime;

public record IntegracaoUsuarioDTO(
        Long id,
        String nomeUsuario,
        String nomeCompleto,
        String email,
        String tipoAcesso,
        Boolean bloqueado,
        LocalDateTime criadoEm
) {
    public static IntegracaoUsuarioDTO from(Usuario u) {
        return new IntegracaoUsuarioDTO(
                u.getId(), u.getNomeUsuario(), u.getNomeCompleto(),
                u.getEmail(), u.getTipoAcesso(), u.getBloqueado(), u.getCriadoEm()
        );
    }
}