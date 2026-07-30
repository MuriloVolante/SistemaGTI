package com.gti.usuarios.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gti.usuarios.audit.AuditoriaListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chamados")
@EntityListeners(AuditoriaListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(length = 50)
    private String categoria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    @JsonIgnoreProperties({"senha", "descricao", "email", "bloqueado", "criadoEm", "atualizadoEm"})
    private Usuario solicitante;

    @ManyToOne
    @JoinColumn(name = "tecnico_id")
    @JsonIgnoreProperties({"senha", "descricao", "email", "bloqueado", "criadoEm", "atualizadoEm"})
    private Usuario tecnico;

    @ManyToOne
    @JoinColumn(name = "ativo_id")
    @JsonIgnoreProperties({"tipo", "responsavel", "centroCusto", "dataCompra",
            "garantiaAte", "vidaUtilMeses", "valorAquisicao", "cadastradoEm", "atualizadoEm"})
    private Ativo ativo;

    @Column(nullable = false, length = 15)
    private String prioridade = "MEDIA";

    @Column(nullable = false, length = 20)
    private String status = "ABERTO";

    @Column(name = "data_abertura", nullable = false, updatable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    @PrePersist
    public void prePersist() {
        this.dataAbertura = LocalDateTime.now();
        if (this.status == null) this.status = "ABERTO";
        if (this.prioridade == null) this.prioridade = "MEDIA";
    }
}