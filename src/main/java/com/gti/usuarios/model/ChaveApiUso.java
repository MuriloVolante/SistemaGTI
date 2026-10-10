package com.gti.usuarios.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chaves_api_uso", indexes = {
        @Index(name = "idx_uso_chave_data", columnList = "chave_id,ocorrido_em"),
        @Index(name = "idx_uso_data", columnList = "ocorrido_em"),
        @Index(name = "idx_uso_sucesso", columnList = "sucesso,ocorrido_em")
})
public class ChaveApiUso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_id")
    private Long chaveId;

    @Column(name = "chave_nome", length = 120)
    private String chaveNome;

    @Column(name = "prefixo_tentado", length = 32)
    private String prefixoTentado;

    @Column(length = 200)
    private String endpoint;

    @Column(length = 10)
    private String metodo;

    @Column(length = 60)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "status_http")
    private Integer statusHttp;

    @Column(nullable = false)
    private boolean sucesso;

    @Column(name = "motivo_negacao", length = 60)
    private String motivoNegacao;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm = LocalDateTime.now();

    public Long getId()                         { return id; }
    public Long getChaveId()                    { return chaveId; }
    public void setChaveId(Long v)              { this.chaveId = v; }
    public String getChaveNome()                { return chaveNome; }
    public void setChaveNome(String v)          { this.chaveNome = v; }
    public String getPrefixoTentado()           { return prefixoTentado; }
    public void setPrefixoTentado(String v)     { this.prefixoTentado = v; }
    public String getEndpoint()                 { return endpoint; }
    public void setEndpoint(String v)           { this.endpoint = v; }
    public String getMetodo()                   { return metodo; }
    public void setMetodo(String v)             { this.metodo = v; }
    public String getIp()                       { return ip; }
    public void setIp(String v)                 { this.ip = v; }
    public String getUserAgent()                { return userAgent; }
    public void setUserAgent(String v)          { this.userAgent = v; }
    public Integer getStatusHttp()              { return statusHttp; }
    public void setStatusHttp(Integer v)        { this.statusHttp = v; }
    public boolean isSucesso()                  { return sucesso; }
    public void setSucesso(boolean v)           { this.sucesso = v; }
    public String getMotivoNegacao()            { return motivoNegacao; }
    public void setMotivoNegacao(String v)      { this.motivoNegacao = v; }
    public LocalDateTime getOcorridoEm()        { return ocorridoEm; }
    public void setOcorridoEm(LocalDateTime v)  { this.ocorridoEm = v; }
}