package com.gti.usuarios.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensagens_chamado")
@Getter
@Setter
@NoArgsConstructor
public class MensagemChamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chamado_id", nullable = false)
    private Long chamadoId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    @JsonIgnoreProperties({"senha", "descricao", "email", "bloqueado", "criadoEm", "atualizadoEm"})
    private Usuario autor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Column(nullable = false, length = 20)
    private String tipo = "COMUM";

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        if (this.tipo == null) this.tipo = "COMUM";
    }
}