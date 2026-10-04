package com.gti.usuarios.service;

import com.gti.usuarios.model.*;
import com.gti.usuarios.repository.*;
import com.gti.usuarios.model.HistoricoAtivo;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final AtivoValidator            validator;
    private final CentroCustoRepository     centroCustoRepo;

    public AtivoService(AtivoRepository ativoRepo,
                        TipoAtivoRepository tipoRepo,
                        UsuarioRepository usuarioRepo,
                        CampoDinamicoRepository campoRepo,
                        ValorCampoRepository valorRepo,
                        HistoricoAtivoRepository historicoRepo,
                        AnexoAtivoRepository anexoRepo,
                        ManutencaoRepository manutencaoRepo,
                        ChamadoRepository chamadoRepo,
                        ChamadoService chamadoService,
                        AtivoValidator validator,
                        CentroCustoRepository centroCustoRepo) {
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
        this.validator      = validator;
        this.centroCustoRepo = centroCustoRepo;
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

    public Ativo buscarPorPatrimonio(String patrimonio) {
        log.debug("Buscando ativo por patrimonio {}", patrimonio);
        return ativoRepo.findByPatrimonio(patrimonio)
                .orElseThrow(() -> {
                    log.warn("Ativo nao encontrado. Patrimonio: {}", patrimonio);
                    return new RuntimeException("Ativo não encontrado. Patrimônio: " + patrimonio);
                });
    }

    public List<ValorCampo> buscarValores(Long ativoId) {
        log.debug("Buscando valores dinamicos do ativo ID {}", ativoId);
        return valorRepo.findByAtivoId(ativoId);
    }

    public String proximaMatricula() {
        long max = ativoRepo.listarMatriculas().stream()
                .filter(p -> p != null && p.matches("[0-9]+"))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0);
        return String.valueOf(max + 1);
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
            h.setUsuarioId(usuarioAtualId());
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

    private Long usuarioAtualId() {
        try {
            String nomeUsuario = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return usuarioRepo.findByNomeUsuario(nomeUsuario).map(Usuario::getId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static Long idObrigatorio(Object valor, String campo) {
        if (valor == null || valor.toString().isBlank())
            throw new RuntimeException(campo + " é obrigatório.");
        try {
            long id = Long.parseLong(valor.toString().trim());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new RuntimeException(campo + " inválido.");
        }
    }

    private static Long idOpcional(Object valor, String campo) {
        if (valor == null || valor.toString().isBlank()) return null;
        try {
            long id = Long.parseLong(valor.toString().trim());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new RuntimeException(campo + " inválido.");
        }
    }

    private static String textoDe(Object valor, String campo) {
        if (valor == null) return null;
        if (!(valor instanceof String s))
            throw new RuntimeException(campo + " inválido.");
        return s;
    }

    private static java.time.LocalDate dataDe(Object valor, String campo) {
        if (valor == null || valor.toString().isBlank()) return null;
        try {
            return java.time.LocalDate.parse(valor.toString().trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw new RuntimeException(campo + " inválida. Use o formato AAAA-MM-DD.");
        }
    }

    private static java.math.BigDecimal decimalDe(Object valor, String campo) {
        if (valor == null || valor.toString().isBlank()) return null;
        try {
            return new java.math.BigDecimal(valor.toString().trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException(campo + " inválido.");
        }
    }

    private void preencherAtivo(Ativo ativo, Map<String, Object> dados) {
        Long tipoId = idObrigatorio(dados.get("tipoId"), "Tipo de ativo");
        TipoAtivo tipo = tipoRepo.findById(tipoId)
                .orElseThrow(() -> new RuntimeException("Tipo de ativo não encontrado. ID: " + tipoId));
        ativo.setTipo(tipo);

        String patrimonio = textoDe(dados.get("patrimonio"), "Patrimônio");
        if (patrimonio == null || patrimonio.isBlank())
            throw new RuntimeException("Patrimônio é obrigatório.");
        String matricula = patrimonio.trim();
        validator.validarMatricula(matricula);
        boolean matriculaDuplicada = ativo.getId() == null
                ? ativoRepo.existsByPatrimonio(matricula)
                : ativoRepo.existsByPatrimonioAndIdNot(matricula, ativo.getId());
        if (matriculaDuplicada)
            throw new RuntimeException("Patrimônio já cadastrada.");
        ativo.setPatrimonio(matricula);

        String marcaModelo = textoDe(dados.get("marcaModelo"), "Marca/Modelo");
        if (marcaModelo == null || marcaModelo.isBlank())
            throw new RuntimeException("Marca/Modelo é obrigatório.");
        validator.validarMarcaModelo(marcaModelo);
        ativo.setMarcaModelo(marcaModelo.trim());

        String centroCusto = textoDe(dados.get("centroCusto"), "Centro de custo");
        if (centroCusto == null || centroCusto.isBlank())
            throw new RuntimeException("Centro de custo é obrigatório.");
        String centroCustoNormalizado = CentroCustoService.normalizar(centroCusto);
        if (!centroCustoRepo.existsByNome(centroCustoNormalizado))
            throw new RuntimeException("Centro de custo não cadastrado.");
        ativo.setCentroCusto(centroCustoNormalizado);

        java.time.LocalDate dataCompra = dataDe(dados.get("dataCompra"), "Data de compra");
        if (dataCompra == null)
            throw new RuntimeException("Data de compra é obrigatória.");
        validator.validarDataCompra(dataCompra);
        ativo.setDataCompra(dataCompra);

        String status = textoDe(dados.get("status"), "Status");
        ativo.setStatus(status == null || status.isBlank() ? "ATIVO" : status.trim().toUpperCase());
        validator.validarStatus(ativo.getStatus());

        Long responsavelId = idOpcional(dados.get("responsavelId"), "Responsável");
        if (responsavelId != null) {
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

        ativo.setGarantiaAte(dataDe(dados.get("garantiaAte"), "Data de garantia"));
        validator.validarGarantia(ativo.getDataCompra(), ativo.getGarantiaAte());

        ativo.setValorAquisicao(decimalDe(dados.get("valorAquisicao"), "Valor de aquisição"));
        validator.validarValorAquisicao(ativo.getValorAquisicao());
    }

    @SuppressWarnings("unchecked")
    private void salvarCamposDinamicos(Ativo ativo, Map<String, Object> dados) {
        Object camposObj = dados.get("camposDinamicos");
        if (camposObj == null) return;
        if (!(camposObj instanceof Map))
            throw new RuntimeException("Campos dinâmicos inválidos.");

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

            String texto = valor.toString().trim();
            if (texto.length() > 1000)
                throw new RuntimeException("Campo '" + campo.getNomeDoCampo() + "' deve ter no máximo 1000 caracteres.");
            validator.validarCampoDinamico(campo.getNomeDoCampo(), campo.getTipoDado(), texto);

            ValorCampo vc = new ValorCampo();
            vc.setAtivo(ativo);
            vc.setCampo(campo);
            vc.setValor(texto);
            valores.add(vc);
        }

        valorRepo.saveAll(valores);
        log.debug("Salvos {} valores dinamicos para ativo ID {}", valores.size(), ativo.getId());
    }
}