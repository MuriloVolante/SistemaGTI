package com.gti.usuarios.repository;

import com.gti.usuarios.model.ChaveApiUso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChaveApiUsoRepository extends JpaRepository<ChaveApiUso, Long> {

    List<ChaveApiUso> findTop50ByChaveIdOrderByOcorridoEmDesc(Long chaveId);

    Page<ChaveApiUso> findBySucessoFalseOrderByOcorridoEmDesc(Pageable pageable);

    Page<ChaveApiUso> findByChaveIdOrderByOcorridoEmDesc(Long chaveId, Pageable pageable);

    long countByChaveIdAndOcorridoEmAfter(Long chaveId, LocalDateTime desde);

    long countByChaveIdAndSucessoFalseAndOcorridoEmAfter(Long chaveId, LocalDateTime desde);

    @Query("select function('date', u.ocorridoEm) as dia, " +
            "count(u) as total, " +
            "sum(case when u.sucesso = false then 1 else 0 end) as negadas " +
            "from ChaveApiUso u " +
            "where u.chaveId = :chaveId and u.ocorridoEm >= :desde " +
            "group by function('date', u.ocorridoEm) " +
            "order by function('date', u.ocorridoEm)")
    List<Object[]> agruparPorDia(@Param("chaveId") Long chaveId,
                                 @Param("desde") LocalDateTime desde);

    @Query("select u.chaveId as chaveId, " +
            "count(u) as total, " +
            "sum(case when u.sucesso = false then 1 else 0 end) as negadas " +
            "from ChaveApiUso u " +
            "where u.chaveId is not null and u.ocorridoEm >= :inicio and u.ocorridoEm < :fim " +
            "group by u.chaveId")
    List<Object[]> consolidarPeriodo(@Param("inicio") LocalDateTime inicio,
                                     @Param("fim") LocalDateTime fim);

    @Modifying
    @Query("delete from ChaveApiUso u where u.ocorridoEm < :limite")
    int apagarAnterioresA(@Param("limite") LocalDateTime limite);
}