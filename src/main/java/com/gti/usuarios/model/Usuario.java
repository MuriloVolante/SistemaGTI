package com.gti.usuarios.model;

import com.gti.usuarios.audit.AuditoriaListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@EntityListeners(AuditoriaListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_usuario", nullable = false, unique = true, length = 100)
    private String nomeUsuario;

    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false, length = 255)
    private String senha;

    @Column(length = 150)
    private String email;

    @Column(name = "tipo_acesso", nullable = false, length = 10)
    private String tipoAcesso = "COMUM"; // COMUM ou TI

    @Column(nullable = false)
    private Boolean bloqueado = false;

    @Column(name = "precisa_trocar_senha", nullable = false)
    private Boolean precisaTrocarSenha = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
        if (this.bloqueado == null) this.bloqueado = false;
        if (this.tipoAcesso == null) this.tipoAcesso = "COMUM";
        if (this.precisaTrocarSenha == null) this.precisaTrocarSenha = true;
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}