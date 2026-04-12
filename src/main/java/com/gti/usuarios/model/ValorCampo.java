package com.gti.usuarios.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "valores_dos_campos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ativo_id", "campo_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ValorCampo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ativo_id", nullable = false)
    @JsonIgnoreProperties({"tipo", "responsavel", "centroCusto", "status",
            "dataCompra", "garantiaAte", "vidaUtilMeses",
            "valorAquisicao", "cadastradoEm", "atualizadoEm"})
    private Ativo ativo;

    @ManyToOne
    @JoinColumn(name = "campo_id", nullable = false)
    @JsonIgnoreProperties({"tipoAtivo"})
    private CampoDinamico campo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String valor;
}