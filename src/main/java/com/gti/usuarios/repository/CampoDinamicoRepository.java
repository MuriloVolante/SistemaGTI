package com.gti.usuarios.repository;

import com.gti.usuarios.model.CampoDinamico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampoDinamicoRepository extends JpaRepository<CampoDinamico, Long> {
    List<CampoDinamico> findByTipoAtivoId(Long tipoId);
}