package com.gti.usuarios.service;

import com.gti.usuarios.model.*;
import com.gti.usuarios.repository.*;
import com.gti.usuarios.model.HistoricoAtivo;
import org.springframework.web.multipart.MultipartFile;
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
    private final AnexoAtivoRepository      anexoRepo;
    private final ManutencaoRepository      manutencaoRepo;
    private final ChamadoRepository         chamadoRepo;
    private final ChamadoService            chamadoService;

    public AtivoService(AtivoRepository ativoRepo,
                        TipoAtivoRepository tipoRepo,
                        UsuarioRepository usuarioRepo,
                        CampoDinamicoRepository campoRepo,
                        ValorCampoRepository valorRepo,
                        HistoricoAtivoRepository historicoRepo,
                        AnexoAtivoRepository anexoRepo,
                        ManutencaoRepository manutencaoRepo,
                        ChamadoRepository chamadoRepo,
                        ChamadoService chamadoService) {
        this.ativoRepo      = ativoRepo;
        this.tipoRepo       = tipoRepo;
        this.usuarioRepo    = usuarioRepo;
        this.campoRepo      = campoRepo;
        this.valorRepo      = valorRepo;
        this.historicoRepo  = historicoRepo;
        this.anexoRepo      = anexoRepo;
        this.manutencaoRepo = manutencaoRepo;
        this.chamadoRepo    = chamadoRepo;
        this.chamadoService = chamadoService;
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

// ── Anexos ────────────────────────────────────────────────────────────────

    @Transactional
    public AnexoAtivo salvarAnexo(Long ativoId, MultipartFile arquivo) {
        buscarPorId(ativoId);

        if (arquivo == null || arquivo.isEmpty())
            throw new RuntimeException("Arquivo vazio.");

        String nome = arquivo.getOriginalFilename();
        String ext = nome != null && nome.contains(".")
                ? nome.substring(nome.lastIndexOf('.') + 1).toLowerCase() : "";

        String tipoConteudo = switch (ext) {
            case "pdf"  -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> null;
        };
        if (tipoConteudo == null)
            throw new RuntimeException("Extensão não permitida. Extensões aceitas: PDF, JPG, JPEG, PNG, DOCX.");

        try {
            AnexoAtivo anexo = new AnexoAtivo();
            anexo.setAtivoId(ativoId);
            anexo.setNome(nome != null ? nome : "anexo." + ext);
            anexo.setConteudo(arquivo.getBytes());
            anexo.setTamanho(arquivo.getSize());
            anexo.setTipoConteudo(tipoConteudo);
            AnexoAtivo salvo = anexoRepo.save(anexo);
            log.info("Anexo salvo. ID: {} | ativo: {} | nome: {}", salvo.getId(), ativoId, salvo.getNome());
            return salvo;
        } catch (java.io.IOException e) {
            throw new RuntimeException("Falha ao ler o arquivo.");
        }
    }

    public List<AnexoMeta> listarAnexos(Long ativoId) {
        return anexoRepo.findMetaByAtivoIdOrderByCriadoEmAsc(ativoId);
    }

    public AnexoAtivo buscarAnexo(Long anexoId) {
        return anexoRepo.findById(anexoId)
                .orElseThrow(() -> new RuntimeException("Anexo não encontrado. ID: " + anexoId));
    }

    @Transactional
    public void excluirAnexo(Long anexoId) {
        AnexoAtivo anexo = buscarAnexo(anexoId);
        anexoRepo.delete(anexo);
        log.info("Anexo removido. ID: {}", anexoId);
    }

    @Transactional
    public void excluirFisico(Long id) {
        buscarPorId(id);

        chamadoRepo.findByAtivoIdOrderByDataAberturaDesc(id)
                .forEach(c -> chamadoService.excluirFisico(c.getId()));

        valorRepo.deleteByAtivoId(id);
        anexoRepo.deleteByAtivoId(id);
        historicoRepo.deleteByAtivoId(id);
        manutencaoRepo.deleteByAtivoId(id);
        valorRepo.flush();

        ativoRepo.deleteById(id);
        log.info("Ativo {} excluído fisicamente", id);
    }

    public Map<String, Object> impactoExclusao(Long id) {
        buscarPorId(id);

        List<Chamado> chamados = chamadoRepo.findByAtivoIdOrderByDataAberturaDesc(id);
        long mensagens = chamados.stream()
                .mapToLong(c -> (Long) chamadoService.impactoExclusao(c.getId()).get("mensagens"))
                .sum();

        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("ativos", 1L);
        m.put("chamados", (long) chamados.size());
        m.put("mensagens", mensagens);
        m.put("camposPreenchidos", (long) valorRepo.findByAtivoId(id).size());
        m.put("anexos", anexoRepo.countByAtivoId(id));
        m.put("historico", historicoRepo.countByAtivoId(id));
        m.put("manutencoes", manutencaoRepo.countByAtivoId(id));
        return m;
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
            boolean mesmoResponsavel = ativo.getResponsavel() != null
                    && ativo.getResponsavel().getId().equals(responsavelId);
            if (Boolean.TRUE.equals(responsavel.getBloqueado()) && !mesmoResponsavel)
                throw new RuntimeException("Não é possível atribuir o ativo a um usuário desativado.");
            ativo.setResponsavel(responsavel);
        } else {
            ativo.setResponsavel(null);
        }

        String garantiaAteStr = (String) dados.get("garantiaAte");
        ativo.setGarantiaAte(garantiaAteStr != null && !garantiaAteStr.isBlank()
                ? java.time.LocalDate.parse(garantiaAteStr) : null);

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