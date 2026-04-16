package com.gti.usuarios.repository;

import com.gti.usuarios.model.Manutencao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {
    List<Manutencao> findByAtivoIdOrderByDataManutencaoDesc(Long ativoId);
}