package com.gti.usuarios.repository;

import com.gti.usuarios.model.TipoAtivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoAtivoRepository extends JpaRepository<TipoAtivo, Long> {
}