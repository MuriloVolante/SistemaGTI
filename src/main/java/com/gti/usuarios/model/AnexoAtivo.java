package com.gti.usuarios.model;

import com.gti.usuarios.audit.AuditoriaListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "anexo_ativo")
@EntityListeners(AuditoriaListener.class)
@Getter
@Setter
@NoArgsConstructor
public class AnexoAtivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ativo_id", nullable = false)
    private Long ativoId;

    @Column(nullable = false, length = 255)
    private String nome;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] conteudo;

    @Column(nullable = false)
    private Long tamanho;

    @Column(name = "tipo_conteudo", nullable = false, length = 100)
    private String tipoConteudo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}