package com.gti.usuarios.repository;

import com.gti.usuarios.model.Ativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AtivoRepository extends JpaRepository<Ativo, Long> {
    List<Ativo> findByTipoId(Long tipoId);
    List<Ativo> findByResponsavelId(Long responsavelId);
    long countByResponsavelId(Long responsavelId);
    boolean existsByPatrimonio(String patrimonio);
    boolean existsByPatrimonioAndIdNot(String patrimonio, Long id);
    Optional<Ativo> findByPatrimonio(String patrimonio);

    @Query("SELECT a.patrimonio FROM Ativo a")
    List<String> listarMatriculas();
}