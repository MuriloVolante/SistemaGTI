package com.gti.usuarios.repository;

import com.gti.usuarios.model.ChaveApi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChaveApiRepository extends JpaRepository<ChaveApi, Long> {

    Optional<ChaveApi> findByHashChave(String hashChave);

    boolean existsByNomeIgnoreCaseAndAtivaTrue(String nome);

    boolean existsByNomeIgnoreCaseAndAtivaTrueAndIdNot(String nome, Long id);

    List<ChaveApi> findAllByOrderByAtivaDescCriadoEmDesc();

    List<ChaveApi> findByAtivaTrue();

    long countByAtivaTrue();
}