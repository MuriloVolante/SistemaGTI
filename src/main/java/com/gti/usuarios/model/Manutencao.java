package com.gti.usuarios.model;

import com.gti.usuarios.audit.AuditoriaListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "manutencoes")
@EntityListeners(AuditoriaListener.class)
public class Manutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ativo_id", nullable = false)
    private Long ativoId;

    @Column(nullable = false)
    private String descricao;

    private String tipo;

    private String tecnico;

    private BigDecimal custo;

    private Boolean garantia;

    @Column(name = "data_manutencao", nullable = false)
    private LocalDate dataManutencao;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Long getId()                        { return id; }
    public Long getAtivoId()                   { return ativoId; }
    public void setAtivoId(Long v)             { this.ativoId = v; }
    public String getDescricao()               { return descricao; }
    public void setDescricao(String v)         { this.descricao = v; }
    public String getTipo()                    { return tipo; }
    public void setTipo(String v)              { this.tipo = v; }
    public String getTecnico()                 { return tecnico; }
    public void setTecnico(String v)           { this.tecnico = v; }
    public BigDecimal getCusto()               { return custo; }
    public void setCusto(BigDecimal v)         { this.custo = v; }
    public Boolean getGarantia()               { return garantia; }
    public void setGarantia(Boolean v)         { this.garantia = v; }
    public LocalDate getDataManutencao()       { return dataManutencao; }
    public void setDataManutencao(LocalDate v) { this.dataManutencao = v; }
    public LocalDateTime getCriadoEm()         { return criadoEm; }
}