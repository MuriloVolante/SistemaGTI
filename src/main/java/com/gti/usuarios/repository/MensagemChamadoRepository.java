package com.gti.usuarios.repository;

import com.gti.usuarios.model.MensagemChamado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensagemChamadoRepository extends JpaRepository<MensagemChamado, Long> {
    List<MensagemChamado> findByChamadoIdOrderByCriadoEmAsc(Long chamadoId);
    long countByChamadoId(Long chamadoId);
}