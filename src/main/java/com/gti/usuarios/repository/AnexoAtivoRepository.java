package com.gti.usuarios.repository;

import com.gti.usuarios.model.AnexoAtivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnexoAtivoRepository extends JpaRepository<AnexoAtivo, Long> {
    List<AnexoMeta> findMetaByAtivoIdOrderByCriadoEmAsc(Long ativoId);
    long countByAtivoId(Long ativoId);
    void deleteByAtivoId(Long ativoId);
}