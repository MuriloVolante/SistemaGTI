package com.gti.usuarios.dto;

import java.time.LocalDate;
import java.util.List;

public record ChaveApiDetalheDTO(
        ChaveApiDTO chave,
        long totalUltimos30Dias,
        long negadasUltimos30Dias,
        List<PontoUso> serie,
        List<ChaveApiUsoDTO> ultimasChamadas
) {
    public record PontoUso(LocalDate dia, long total, long negadas) {}
}