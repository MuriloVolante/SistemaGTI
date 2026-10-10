package com.gti.usuarios.model;

import com.gti.usuarios.audit.AuditoriaListener;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "chaves_api", indexes = {
        @Index(name = "idx_chave_api_hash", columnList = "hash_chave", unique = true),
        @Index(name = "idx_chave_api_prefixo", columnList = "prefixo")
})
@EntityListeners(AuditoriaListener.class)
public class ChaveApi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 32)
    private String prefixo;

    @Column(name = "hash_chave", nullable = false, length = 64)
    private String hashChave;

    @Column(length = 255)
    private String escopos;

    @Column(name = "ips_permitidos", length = 500)
    private String ipsPermitidos;

    @Column(name = "expira_em")
    private LocalDateTime expiraEm;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "criado_por_id")
    private Long criadoPorId;

    @Column(name = "criado_por_nome", length = 120)
    private String criadoPorNome;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "desligada_em")
    private LocalDateTime desligadaEm;

    @Column(name = "desligada_por_nome", length = 120)
    private String desligadaPorNome;

    @Column(name = "ultimo_uso_em")
    private LocalDateTime ultimoUsoEm;

    @Column(name = "ultimo_uso_ip", length = 60)
    private String ultimoUsoIp;

    @Column(name = "substituida_por_id")
    private Long substituidaPorId;

    @Transient
    public List<String> getEscoposLista() {
        if (escopos == null || escopos.isBlank()) return List.of();
        return Arrays.stream(escopos.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Transient
    public List<String> getIpsLista() {
        if (ipsPermitidos == null || ipsPermitidos.isBlank()) return List.of();
        return Arrays.stream(ipsPermitidos.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Transient
    public boolean isExpirada() {
        return expiraEm != null && expiraEm.isBefore(LocalDateTime.now());
    }

    @Transient
    public boolean isUtilizavel() {
        return ativa && !isExpirada();
    }

    public Long getId()                        { return id; }
    public String getNome()                    { return nome; }
    public void setNome(String v)              { this.nome = v; }
    public String getPrefixo()                 { return prefixo; }
    public void setPrefixo(String v)           { this.prefixo = v; }
    public String getHashChave()               { return hashChave; }
    public void setHashChave(String v)         { this.hashChave = v; }
    public String getEscopos()                 { return escopos; }
    public void setEscopos(String v)           { this.escopos = v; }
    public String getIpsPermitidos()           { return ipsPermitidos; }
    public void setIpsPermitidos(String v)     { this.ipsPermitidos = v; }
    public LocalDateTime getExpiraEm()         { return expiraEm; }
    public void setExpiraEm(LocalDateTime v)   { this.expiraEm = v; }
    public boolean isAtiva()                   { return ativa; }
    public void setAtiva(boolean v)            { this.ativa = v; }
    public Long getCriadoPorId()               { return criadoPorId; }
    public void setCriadoPorId(Long v)         { this.criadoPorId = v; }
    public String getCriadoPorNome()           { return criadoPorNome; }
    public void setCriadoPorNome(String v)     { this.criadoPorNome = v; }
    public LocalDateTime getCriadoEm()         { return criadoEm; }
    public void setCriadoEm(LocalDateTime v)   { this.criadoEm = v; }
    public LocalDateTime getDesligadaEm()      { return desligadaEm; }
    public void setDesligadaEm(LocalDateTime v){ this.desligadaEm = v; }
    public String getDesligadaPorNome()        { return desligadaPorNome; }
    public void setDesligadaPorNome(String v)  { this.desligadaPorNome = v; }
    public LocalDateTime getUltimoUsoEm()      { return ultimoUsoEm; }
    public void setUltimoUsoEm(LocalDateTime v){ this.ultimoUsoEm = v; }
    public String getUltimoUsoIp()             { return ultimoUsoIp; }
    public void setUltimoUsoIp(String v)       { this.ultimoUsoIp = v; }
    public Long getSubstituidaPorId()          { return substituidaPorId; }
    public void setSubstituidaPorId(Long v)    { this.substituidaPorId = v; }
}