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

    @Column(name = "vida_util_meses")
    private Integer vidaUtilMeses;

    @Column(name = "percentual_depreciacao", precision = 5, scale = 2)
    private java.math.BigDecimal percentualDepreciacao;

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