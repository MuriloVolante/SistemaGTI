package com.gti.usuarios.repository;

import com.gti.usuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * REPOSITORY — Camada de acesso ao banco
 *
 * O Spring gera automaticamente os métodos:
 * findAll(), findById(), save(), deleteById(), etc.
 *
 * Não precisa escrever SQL para operações básicas.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Exemplo: buscar por nome (não usado agora, mas já disponível)
    // List<Usuario> findByNomeContainingIgnoreCase(String nome);
}
