package com.gti.usuarios.dto;

public record CampoDinamicoRequestDTO(
        String nomeDoCampo,
        String tipoDado,
        Boolean obrigatorio
) {}