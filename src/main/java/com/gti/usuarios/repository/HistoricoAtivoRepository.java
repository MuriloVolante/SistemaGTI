package com.gti.usuarios.repository;

import com.gti.usuarios.model.HistoricoAtivo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoricoAtivoRepository extends JpaRepository<HistoricoAtivo, Long> {
    List<HistoricoAtivo> findByAtivoIdOrderByCriadoEmDesc(Long ativoId);
    long countByAtivoId(Long ativoId);
    void deleteByAtivoId(Long ativoId);
}