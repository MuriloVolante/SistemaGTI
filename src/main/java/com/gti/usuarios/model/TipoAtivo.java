package com.gti.usuarios.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gti.usuarios.audit.AuditoriaListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tipos_de_ativo")
@EntityListeners(AuditoriaListener.class)
@Getter
@Setter
@NoArgsConstructor
public class TipoAtivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @JsonIgnore
    @OneToMany(mappedBy = "tipoAtivo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampoDinamico> campos;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}