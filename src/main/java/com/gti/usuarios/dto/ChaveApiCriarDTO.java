package com.gti.usuarios.dto;

import java.util.List;

public record ChaveApiCriarDTO(
        String nome,
        List<String> escopos,
        Integer diasValidade,
        List<String> ipsPermitidos
) {}