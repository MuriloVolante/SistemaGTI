package com.gti.usuarios.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ativos")
@Getter
@Setter
@NoArgsConstructor
public class Ativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tipo_id", nullable = false)
    private TipoAtivo tipo;

    @Column(nullable = false, unique = true, length = 50)
    private String patrimonio;

    @Column(name = "marca_modelo", nullable = false, length = 150)
    private String marcaModelo;

    @ManyToOne
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @Column(name = "centro_custo", nullable = false, length = 100)
    private String centroCusto;

    @Column(nullable = false, length = 20)
    private String status = "ATIVO"; // ATIVO, MANUTENCAO, ESTOQUE, DESCARTADO

    @Column(name = "data_compra", nullable = false)
    private LocalDate dataCompra;

    @Column(name = "garantia_ate")
    private LocalDate garantiaAte;

    @Column(name = "vida_util_meses")
    private Integer vidaUtilMeses;

    @Column(name = "valor_aquisicao", precision = 12, scale = 2)
    private BigDecimal valorAquisicao;

    @Column(name = "cadastrado_em", nullable = false, updatable = false)
    private LocalDateTime cadastradoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        this.cadastradoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
        if (this.status == null) this.status = "ATIVO";
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}