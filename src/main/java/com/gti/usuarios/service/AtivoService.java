package com.gti.usuarios.service;

import com.gti.usuarios.model.*;
import com.gti.usuarios.repository.*;
import com.gti.usuarios.model.HistoricoAtivo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AtivoService {

    private static final Logger log = LoggerFactory.getLogger(AtivoService.class);

    private final AtivoRepository        ativoRepo;
    private final TipoAtivoRepository    tipoRepo;
    private final UsuarioRepository      usuarioRepo;
    private final CampoDinamicoRepository campoRepo;
    private final ValorCampoRepository      valorRepo;
    private final HistoricoAtivoRepository  historicoRepo;

    public AtivoService(AtivoRepository ativoRepo,
                        TipoAtivoRepository tipoRepo,
                        UsuarioRepository usuarioRepo,
                        CampoDinamicoRepository campoRepo,
                        ValorCampoRepository valorRepo,
                        HistoricoAtivoRepository historicoRepo) {
        this.ativoRepo      = ativoRepo;
        this.tipoRepo       = tipoRepo;
        this.usuarioRepo    = usuarioRepo;
        this.campoRepo      = campoRepo;
        this.valorRepo      = valorRepo;
        this.historicoRepo  = historicoRepo;
    }

    public List<Ativo> listarTodos() {
        log.debug("Listando todos os ativos");
        List<Ativo> ativos = ativoRepo.findAll();
        log.debug("Total de ativos: {}", ativos.size());
        return ativos;
    }

    public Ativo buscarPorId(Long id) {
        log.debug("Buscando ativo ID {}", id);
        return ativoRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Ativo nao encontrado. ID: {}", id);
                    return new RuntimeException("Ativo não encontrado. ID: " + id);
                });
    }

    public List<ValorCampo> buscarValores(Long ativoId) {
        log.debug("Buscando valores dinamicos do ativo ID {}", ativoId);
        return valorRepo.findByAtivoId(ativoId);
    }

    @Transactional
    public Ativo criar(Map<String, Object> dados) {
        log.debug("Criando ativo: {}", dados.get("patrimonio"));

        Ativo ativo = new Ativo();
        preencherAtivo(ativo, dados);
        Ativo salvo = ativoRepo.save(ativo);

        salvarCamposDinamicos(salvo, dados);

        log.info("Ativo criado. ID: {} | patrimonio: {}", salvo.getId(), salvo.getPatrimonio());
        return salvo;
    }

    @Transactional
    public Ativo atualizar(Long id, Map<String, Object> dados) {
        log.debug("Atualizando ativo ID {}", id);

        Ativo ativo = buscarPorId(id);

        // guarda o responsável anterior antes de sobrescrever
        String respAnterior = ativo.getResponsavel() != null
                ? ativo.getResponsavel().getNomeCompleto() : null;

        preencherAtivo(ativo, dados);
        Ativo salvo = ativoRepo.save(ativo);

        // se o responsável mudou, registra no histórico
        String respNovo = salvo.getResponsavel() != null
                ? salvo.getResponsavel().getNomeCompleto() : null;

        if (!java.util.Objects.equals(respAnterior, respNovo)) {
            HistoricoAtivo h = new HistoricoAtivo();
            h.setAtivoId(salvo.getId());
            h.setTipoEvento("MUDANCA_RESPONSAVEL");
            h.setDescricao("Responsável alterado de "
                    + (respAnterior != null ? respAnterior : "nenhum")
                    + " para "
                    + (respNovo != null ? respNovo : "nenhum") + ".");
            historicoRepo.save(h);
        }

        // Força o flush do delete antes de inserir os novos valores
        valorRepo.deleteByAtivoId(salvo.getId());
        valorRepo.flush();

        salvarCamposDinamicos(salvo, dados);

        log.info("Ativo atualizado. ID: {}", salvo.getId());
        return salvo;
    }

    @Transactional
    public void excluir(Long id) {
        log.debug("Excluindo ativo ID {}", id);
        buscarPorId(id);
        valorRepo.deleteByAtivoId(id);
        ativoRepo.deleteById(id);
        log.info("Ativo ID {} excluido", id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void preencherAtivo(Ativo ativo, Map<String, Object> dados) {
        Long tipoId = Long.valueOf(dados.get("tipoId").toString());
        TipoAtivo tipo = tipoRepo.findById(tipoId)
                .orElseThrow(() -> new RuntimeException("Tipo de ativo não encontrado. ID: " + tipoId));
        ativo.setTipo(tipo);

        String patrimonio = (String) dados.get("patrimonio");
        if (patrimonio == null || patrimonio.isBlank())
            throw new RuntimeException("Patrimônio é obrigatório.");
        ativo.setPatrimonio(patrimonio.trim());

        String marcaModelo = (String) dados.get("marcaModelo");
        if (marcaModelo == null || marcaModelo.isBlank())
            throw new RuntimeException("Marca/Modelo é obrigatório.");
        ativo.setMarcaModelo(marcaModelo.trim());

        String centroCusto = (String) dados.get("centroCusto");
        if (centroCusto == null || centroCusto.isBlank())
            throw new RuntimeException("Centro de custo é obrigatório.");
        ativo.setCentroCusto(centroCusto.trim());

        String dataCompraStr = (String) dados.get("dataCompra");
        if (dataCompraStr == null || dataCompraStr.isBlank())
            throw new RuntimeException("Data de compra é obrigatória.");
        ativo.setDataCompra(java.time.LocalDate.parse(dataCompraStr));

        String status = (String) dados.get("status");
        ativo.setStatus(status == null || status.isBlank() ? "ATIVO" : status);

        Object responsavelIdObj = dados.get("responsavelId");
        if (responsavelIdObj != null && !responsavelIdObj.toString().isBlank()) {
            Long responsavelId = Long.valueOf(responsavelIdObj.toString());
            Usuario responsavel = usuarioRepo.findById(responsavelId)
                    .orElseThrow(() -> new RuntimeException("Responsável não encontrado."));
            ativo.setResponsavel(responsavel);
        } else {
            ativo.setResponsavel(null);
        }

        String garantiaAteStr = (String) dados.get("garantiaAte");
        ativo.setGarantiaAte(garantiaAteStr != null && !garantiaAteStr.isBlank()
                ? java.time.LocalDate.parse(garantiaAteStr) : null);

        Object vidaUtilObj = dados.get("vidaUtilMeses");
        ativo.setVidaUtilMeses(vidaUtilObj != null && !vidaUtilObj.toString().isBlank()
                ? Integer.valueOf(vidaUtilObj.toString()) : null);

        Object valorObj = dados.get("valorAquisicao");
        ativo.setValorAquisicao(valorObj != null && !valorObj.toString().isBlank()
                ? new java.math.BigDecimal(valorObj.toString()) : null);
    }

    @SuppressWarnings("unchecked")
    private void salvarCamposDinamicos(Ativo ativo, Map<String, Object> dados) {
        Object camposObj = dados.get("camposDinamicos");
        if (camposObj == null) return;

        Map<String, Object> camposDinamicos = (Map<String, Object>) camposObj;
        List<CampoDinamico> camposDoTipo = campoRepo.findByTipoAtivoId(ativo.getTipo().getId());

        // Valida obrigatorios
        for (CampoDinamico campo : camposDoTipo) {
            if (campo.getObrigatorio()) {
                Object valor = camposDinamicos.get(campo.getId().toString());
                if (valor == null || valor.toString().isBlank()) {
                    throw new RuntimeException("Campo obrigatório não preenchido: " + campo.getNomeDoCampo());
                }
            }
        }

        // Salva cada valor
        List<ValorCampo> valores = new ArrayList<>();
        for (CampoDinamico campo : camposDoTipo) {
            Object valor = camposDinamicos.get(campo.getId().toString());
            if (valor == null || valor.toString().isBlank()) continue;

            ValorCampo vc = new ValorCampo();
            vc.setAtivo(ativo);
            vc.setCampo(campo);
            vc.setValor(valor.toString());
            valores.add(vc);
        }

        valorRepo.saveAll(valores);
        log.debug("Salvos {} valores dinamicos para ativo ID {}", valores.size(), ativo.getId());
    }
}