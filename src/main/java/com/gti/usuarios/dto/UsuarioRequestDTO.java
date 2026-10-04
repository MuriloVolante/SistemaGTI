package com.gti.usuarios.dto;

public record UsuarioRequestDTO(
        String nomeUsuario,
        String nomeCompleto,
        String descricao,
        String email,
        String tipoAcesso,
        String senha,
        Boolean precisaTrocarSenha
) {}