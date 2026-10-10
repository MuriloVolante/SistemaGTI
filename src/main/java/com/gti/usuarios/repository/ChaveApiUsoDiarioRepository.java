package com.gti.usuarios.repository;

import com.gti.usuarios.model.ChaveApiUsoDiario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ChaveApiUsoDiarioRepository extends JpaRepository<ChaveApiUsoDiario, Long> {

    Optional<ChaveApiUsoDiario> findByChaveIdAndDia(Long chaveId, LocalDate dia);

    List<ChaveApiUsoDiario> findByChaveIdAndDiaBetweenOrderByDia(Long chaveId, LocalDate inicio, LocalDate fim);

    void deleteByChaveId(Long chaveId);
}