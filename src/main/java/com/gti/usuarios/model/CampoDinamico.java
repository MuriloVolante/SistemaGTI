package com.gti.usuarios.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "campos_do_tipo")
@Getter
@Setter
@NoArgsConstructor
public class CampoDinamico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnoreProperties({"campos", "criadoEm"})
    @ManyToOne
    @JoinColumn(name = "tipo_id", nullable = false)
    private TipoAtivo tipoAtivo;

    @Column(name = "nome_do_campo", nullable = false, length = 100)
    private String nomeDoCampo;

    @Column(name = "tipo_dado", nullable = false, length = 20)
    private String tipoDado;

    @Column(nullable = false)
    private Boolean obrigatorio = false;
}