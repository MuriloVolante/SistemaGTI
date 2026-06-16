package com.gti.usuarios.repository;

import com.gti.usuarios.model.Ativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtivoRepository extends JpaRepository<Ativo, Long> {
    List<Ativo> findByStatus(String status);
    List<Ativo> findByTipoId(Long tipoId);
    long countByResponsavelId(Long responsavelId);
}