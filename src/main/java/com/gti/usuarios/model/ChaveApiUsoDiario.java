package com.gti.usuarios.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "chaves_api_uso_diario", uniqueConstraints = {
        @UniqueConstraint(name = "uk_uso_diario_chave_dia", columnNames = {"chave_id", "dia"})
})
public class ChaveApiUsoDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_id", nullable = false)
    private Long chaveId;

    @Column(nullable = false)
    private LocalDate dia;

    @Column(name = "total_chamadas", nullable = false)
    private long totalChamadas = 0;

    @Column(name = "total_negadas", nullable = false)
    private long totalNegadas = 0;

    public Long getId()                    { return id; }
    public Long getChaveId()               { return chaveId; }
    public void setChaveId(Long v)         { this.chaveId = v; }
    public LocalDate getDia()              { return dia; }
    public void setDia(LocalDate v)        { this.dia = v; }
    public long getTotalChamadas()         { return totalChamadas; }
    public void setTotalChamadas(long v)   { this.totalChamadas = v; }
    public long getTotalNegadas()          { return totalNegadas; }
    public void setTotalNegadas(long v)    { this.totalNegadas = v; }
}