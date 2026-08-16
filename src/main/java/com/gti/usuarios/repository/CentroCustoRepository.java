package com.gti.usuarios.repository;

import com.gti.usuarios.model.CentroCusto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CentroCustoRepository extends JpaRepository<CentroCusto, Long> {
    List<CentroCusto> findAllByOrderByNomeAsc();
    Optional<CentroCusto> findByNome(String nome);
    boolean existsByNome(String nome);
}