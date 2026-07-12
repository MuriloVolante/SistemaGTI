package com.gti.usuarios.repository;

import java.time.LocalDateTime;

public interface AnexoMeta {
    Long getId();
    String getNome();
    Long getTamanho();
    LocalDateTime getCriadoEm();
}