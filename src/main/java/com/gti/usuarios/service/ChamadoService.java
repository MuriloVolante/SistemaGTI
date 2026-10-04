package com.gti.usuarios.service;

import com.gti.usuarios.model.Ativo;
import com.gti.usuarios.model.Chamado;
import com.gti.usuarios.model.Usuario;
import com.gti.usuarios.repository.AtivoRepository;
import com.gti.usuarios.repository.ChamadoRepository;
import com.gti.usuarios.repository.MensagemChamadoRepository;
import com.gti.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Service
public class ChamadoService {

    private static final Logger log = LoggerFactory.getLogger(ChamadoService.class);

    private static final List<String> PRIORIDADES = List.of("BAIXA", "MEDIA", "ALTA", "MUITO_ALTA");
    private static final List<String> STATUS = List.of("ABERTO", "EM_ANDAMENTO", "CONCLUIDO");
    private static final List<String> CATEGORIAS = List.of("HARDWARE", "SOFTWARE", "REDE", "ACESSO", "EMAIL", "IMPRESSAO", "TELEFONIA", "OUTRO");

    private static final Map<String, Integer> ORDEM_STATUS =
            Map.of("ABERTO", 0, "EM_ANDAMENTO", 1, "CONCLUIDO", 2);
    private static final Map<String, Integer> ORDEM_PRIORIDADE =
            Map.of("MUITO_ALTA", 0, "ALTA", 1, "MEDIA", 2, "BAIXA", 3);

    private static final Comparator<Chamado> ORDENACAO_PADRAO = Comparator
            .comparingInt((Chamado c) -> ORDEM_STATUS.getOrDefault(c.getStatus(), 99))
            .thenComparingInt(c -> ORDEM_PRIORIDADE.getOrDefault(c.getPrioridade(), 99))
            .thenComparing(Chamado::getDataAbertura, Comparator.nullsLast(Comparator.reverseOrder()));
    private final ChamadoRepository chamadoRepo;
    private final MensagemChamadoRepository mensagemRepo;
    private final MensagemChamadoService mensagemService;
    private final UsuarioRepository usuarioRepo;
    private final AtivoRepository ativoRepo;

    public ChamadoService(ChamadoRepository chamadoRepo,
                          MensagemChamadoRepository mensagemRepo,
                          MensagemChamadoService mensagemService,
                          UsuarioRepository usuarioRepo,
                          AtivoRepository ativoRepo) {
        this.chamadoRepo     = chamadoRepo;
        this.mensagemRepo    = mensagemRepo;
        this.mensagemService = mensagemService;
        this.usuarioRepo     = usuarioRepo;
        this.ativoRepo       = ativoRepo;
    }

    public List<Chamado> listarTodos() {
        return chamadoRepo.findAll().stream()
                .sorted(ORDENACAO_PADRAO)
                .toList();
    }

    public List<Chamado> listarPorSolicitante(Long solicitanteId) {
        return chamadoRepo.findBySolicitanteIdOrderByDataAberturaDesc(solicitanteId).stream()
                .sorted(ORDENACAO_PADRAO)
                .toList();
    }

    public List<Chamado> listarPorAtivo(Long ativoId) {
        return chamadoRepo.findByAtivoIdOrderByDataAberturaDesc(ativoId);
    }

    public Chamado buscarPorId(Long id) {
        return chamadoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Chamado não encontrado. ID: " + id));
    }

    private static final int MIN_TAM_TITULO = 5;
    private static final int MAX_TAM_TITULO = 200;
    private static final int MIN_TAM_DESCRICAO = 10;
    private static final int MAX_TAM_DESCRICAO = 5000;

    private static String limparTexto(String valor, boolean permitirQuebraLinha) {
        if (valor == null) return null;
        String limpo = permitirQuebraLinha
                ? valor.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                : valor.replaceAll("\\p{Cntrl}", " ");
        return limpo.trim().replaceAll("[ \\t]{2,}", " ");
    }

    private String tituloValidado(String bruto) {
        String t = limparTexto(bruto, false);
        if (t == null || t.isEmpty())
            throw new RuntimeException("Título é obrigatório.");
        if (t.length() < MIN_TAM_TITULO)
            throw new RuntimeException("Título deve ter no mínimo " + MIN_TAM_TITULO + " caracteres.");
        if (t.length() > MAX_TAM_TITULO)
            throw new RuntimeException("Título deve ter no máximo " + MAX_TAM_TITULO + " caracteres.");
        return t;
    }

    private String descricaoValidada(String bruto) {
        String d = limparTexto(bruto, true);
        if (d == null || d.isEmpty())
            throw new RuntimeException("Descrição é obrigatória.");
        if (d.length() < MIN_TAM_DESCRICAO)
            throw new RuntimeException("Descrição deve ter no mínimo " + MIN_TAM_DESCRICAO + " caracteres.");
        if (d.length() > MAX_TAM_DESCRICAO)
            throw new RuntimeException("Descrição deve ter no máximo " + MAX_TAM_DESCRICAO + " caracteres.");
        return d;
    }

    private static String textoDe(Object valor, String campo) {
        if (valor == null) return null;
        if (!(valor instanceof String s))
            throw new RuntimeException("Campo " + campo + " inválido.");
        return s;
    }

    private static Long idDe(Object valor, String campo) {
        if (valor == null) return null;
        String s = valor.toString().trim();
        if (s.isEmpty()) return null;
        try {
            long id = Long.parseLong(s);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new RuntimeException("Campo " + campo + " inválido.");
        }
    }

    @Transactional
    public Chamado criar(String titulo, String descricao, Usuario solicitante) {
        if (solicitante == null)
            throw new RuntimeException("Solicitante não identificado.");
        if (Boolean.TRUE.equals(solicitante.getBloqueado()))
            throw new RuntimeException("Conta bloqueada. Contate o administrador.");

        String tituloLimpo = tituloValidado(titulo);
        String descricaoLimpa = descricaoValidada(descricao);

        Chamado c = new Chamado();
        c.setTitulo(tituloLimpo);
        c.setDescricao(descricaoLimpa);
        c.setSolicitante(solicitante);
        c.setStatus("ABERTO");
        c.setPrioridade("MEDIA");

        Chamado salvo = chamadoRepo.save(c);
        log.info("Chamado criado. ID: {} | solicitante ID: {}", salvo.getId(), solicitante.getId());
        return salvo;
    }

    @Transactional
    public Chamado atualizar(Long id, Map<String, Object> dados, Usuario solicitante) {
        Chamado c = buscarPorId(id);
        if (c.getTecnico() == null || !c.getTecnico().getId().equals(solicitante.getId()))
            throw new RuntimeException("Apenas o técnico que assumiu o chamados pode editá-lo.");

        if ("CONCLUIDO".equals(c.getStatus()))
            throw new RuntimeException("Chamado concluído não pode ser editado. Reabra o chamado antes.");

        if (dados.containsKey("titulo"))
            c.setTitulo(tituloValidado(textoDe(dados.get("titulo"), "título")));

        if (dados.containsKey("descricao"))
            c.setDescricao(descricaoValidada(textoDe(dados.get("descricao"), "descrição")));

        if (dados.containsKey("categoria")) {
            String cat = textoDe(dados.get("categoria"), "categoria");
            cat = cat == null ? null : cat.trim().toUpperCase();
            if (cat == null || cat.isEmpty()) {
                c.setCategoria(null);
            } else {
                if (!CATEGORIAS.contains(cat))
                    throw new RuntimeException("Categoria inválida.");
                c.setCategoria(cat);
            }
        }

        if (dados.containsKey("prioridade")) {
            String p = textoDe(dados.get("prioridade"), "prioridade");
            if (p != null && !p.isBlank()) {
                p = p.trim().toUpperCase();
                if (!PRIORIDADES.contains(p))
                    throw new RuntimeException("Prioridade inválida.");
                c.setPrioridade(p);
            }
        }

        if (dados.containsKey("ativoId")) {
            Long ativoId = idDe(dados.get("ativoId"), "ativo");
            if (ativoId == null) {
                c.setAtivo(null);
            } else {
                Ativo a = ativoRepo.findById(ativoId)
                        .orElseThrow(() -> new RuntimeException("Ativo não encontrado."));
                boolean jaVinculado = c.getAtivo() != null && c.getAtivo().getId().equals(ativoId);
                boolean doSolicitante = a.getResponsavel() != null
                        && c.getSolicitante() != null
                        && a.getResponsavel().getId().equals(c.getSolicitante().getId());
                if (!jaVinculado && !doSolicitante)
                    throw new RuntimeException("O ativo vinculado deve estar sob responsabilidade do solicitante do chamado.");
                c.setAtivo(a);
            }
        }

        if (dados.containsKey("tecnicoId")) {
            Long tecnicoId = idDe(dados.get("tecnicoId"), "técnico");
            if (tecnicoId == null) {
                c.setTecnico(null);
                if ("EM_ANDAMENTO".equals(c.getStatus()))
                    c.setStatus("ABERTO");
            } else {
                Usuario t = usuarioRepo.findById(tecnicoId)
                        .orElseThrow(() -> new RuntimeException("Técnico não encontrado."));
                if (!"TI".equals(t.getTipoAcesso()))
                    throw new RuntimeException("Apenas usuários TI podem ser técnicos.");
                if (Boolean.TRUE.equals(t.getBloqueado()))
                    throw new RuntimeException("Não é possível atribuir o chamado a um usuário desativado.");
                c.setTecnico(t);
            }
        }

        Chamado salvo = chamadoRepo.save(c);
        log.info("Chamado atualizado. ID: {}", salvo.getId());
        return salvo;
    }

    @Transactional
    public Chamado assumir(Long id, Usuario tecnico) {
        Chamado c = buscarPorId(id);
        if (!"TI".equals(tecnico.getTipoAcesso()))
            throw new RuntimeException("Apenas usuários TI podem assumir chamados.");
        if ("CONCLUIDO".equals(c.getStatus()))
            throw new RuntimeException("Chamado já concluído.");

        c.setTecnico(tecnico);
        c.setStatus("EM_ANDAMENTO");
        Chamado salvo = chamadoRepo.save(c);
        log.info("Chamado {} assumido por {}", id, tecnico.getNomeUsuario());
        return salvo;
    }

    @Transactional
    public Chamado alterarStatus(Long id, String novoStatus, String mensagem, Usuario solicitante) {
        if (!STATUS.contains(novoStatus))
            throw new RuntimeException("Status inválido.");

        Chamado c = buscarPorId(id);
        if (c.getTecnico() == null || !c.getTecnico().getId().equals(solicitante.getId()))
            throw new RuntimeException("Apenas o técnico que assumiu o chamados pode alterar o status.");

        if ("CONCLUIDO".equals(novoStatus)) {
            if (mensagem == null || mensagem.isBlank())
                throw new RuntimeException("Mensagem de encerramento é obrigatória.");
            mensagemService.registrarTipada(id, mensagem, solicitante, "ENCERRAMENTO");
            c.setDataFechamento(LocalDateTime.now());
        } else {
            c.setDataFechamento(null);
        }

        c.setStatus(novoStatus);
        Chamado salvo = chamadoRepo.save(c);
        log.info("Chamado {} alterado para status {}", id, novoStatus);
        return salvo;
    }

    @Transactional
    public Chamado reabrir(Long id, Usuario solicitante) {
        if (!"TI".equals(solicitante.getTipoAcesso()))
            throw new RuntimeException("Apenas usuários TI podem reabrir chamados.");
        Chamado c = buscarPorId(id);
        if (!"CONCLUIDO".equals(c.getStatus()))
            throw new RuntimeException("Apenas chamados concluídos podem ser reabertos.");
        c.setStatus("ABERTO");
        c.setDataFechamento(null);
        c.setTecnico(null);
        Chamado salvo = chamadoRepo.save(c);
        log.info("Chamado {} reaberto por {}", id, solicitante.getNomeUsuario());
        return salvo;
    }

    @Transactional
    public Chamado alterarAtivo(Long id, Object ativoId, Usuario tecnico) {
        Chamado c = buscarPorId(id);
        if (c.getTecnico() == null || !c.getTecnico().getId().equals(tecnico.getId()))
            throw new RuntimeException("Apenas o técnico que assumiu o chamados pode vincular o ativo.");
        if (ativoId == null || ativoId.toString().isBlank()) {
            c.setAtivo(null);
        } else {
            Ativo a = ativoRepo.findById(Long.valueOf(ativoId.toString()))
                    .orElseThrow(() -> new RuntimeException("Ativo não encontrado."));
            c.setAtivo(a);
        }
        Chamado salvo = chamadoRepo.save(c);
        log.info("Ativo do chamados {} alterado", id);
        return salvo;
    }

    @Transactional
    public void excluir(Long id, Usuario solicitante) {
        Chamado c = buscarPorId(id);
        if (c.getTecnico() == null || !c.getTecnico().getId().equals(solicitante.getId()))
            throw new RuntimeException("Apenas o técnico que assumiu o chamados pode excluí-lo.");
        excluirFisico(id);
    }

    @Transactional
    public void excluirFisico(Long id) {
        mensagemRepo.deleteByChamadoId(id);
        mensagemRepo.flush();
        chamadoRepo.deleteById(id);
        log.info("Chamado {} excluído fisicamente", id);
    }

    public Map<String, Object> impactoExclusao(Long id) {
        buscarPorId(id);
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("chamados", 1L);
        m.put("mensagens", mensagemRepo.countByChamadoId(id));
        return m;
    }
}