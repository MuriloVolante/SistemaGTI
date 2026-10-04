package com.gti.usuarios.dto;

public record TipoAtivoRequestDTO(
        String nome,
        String vidaUtilMeses,
        String percentualDepreciacao
) {}