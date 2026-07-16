package com.gti.usuarios.repository;

import com.gti.usuarios.model.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Long> {
    List<Chamado> findBySolicitanteIdOrderByDataAberturaDesc(Long solicitanteId);
    List<Chamado> findByAtivoIdOrderByDataAberturaDesc(Long ativoId);
    List<Chamado> findByTecnicoId(Long tecnicoId);
    long countBySolicitanteId(Long solicitanteId);
    long countByTecnicoId(Long tecnicoId);
    long countByAtivoId(Long ativoId);
}