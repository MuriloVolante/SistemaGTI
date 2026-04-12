package com.gti.usuarios.repository;

import com.gti.usuarios.model.ValorCampo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ValorCampoRepository extends JpaRepository<ValorCampo, Long> {
    List<ValorCampo> findByAtivoId(Long ativoId);

    @Modifying
    void deleteByAtivoId(Long ativoId);
}