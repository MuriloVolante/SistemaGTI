package com.gti.usuarios.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico_ativos")
public class HistoricoAtivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ativo_id", nullable = false)
    private Long ativoId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "tipo_evento")
    private String tipoEvento;

    private String descricao;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Long getId()                 { return id; }
    public Long getAtivoId()            { return ativoId; }
    public void setAtivoId(Long v)      { this.ativoId = v; }
    public Long getUsuarioId()          { return usuarioId; }
    public void setUsuarioId(Long v)    { this.usuarioId = v; }
    public String getTipoEvento()       { return tipoEvento; }
    public void setTipoEvento(String v) { this.tipoEvento = v; }
    public String getDescricao()        { return descricao; }
    public void setDescricao(String v)  { this.descricao = v; }
    public LocalDateTime getCriadoEm()  { return criadoEm; }
}