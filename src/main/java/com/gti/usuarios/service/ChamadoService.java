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

    @Transactional
    public Chamado criar(String titulo, String descricao, Usuario solicitante) {
        if (titulo == null || titulo.isBlank())
            throw new RuntimeException("Título é obrigatório.");
        if (descricao == null || descricao.isBlank())
            throw new RuntimeException("Descrição é obrigatória.");

        Chamado c = new Chamado();
        c.setTitulo(titulo.trim());
        c.setDescricao(descricao.trim());
        c.setSolicitante(solicitante);
        c.setStatus("ABERTO");
        c.setPrioridade("MEDIA");

        Chamado salvo = chamadoRepo.save(c);
        log.info("Chamado criado. ID: {} | solicitante: {}", salvo.getId(), solicitante.getNomeUsuario());
        return salvo;
    }

    @Transactional
    public Chamado atualizar(Long id, Map<String, Object> dados, Usuario solicitante) {
        Chamado c = buscarPorId(id);
        if (c.getTecnico() == null || !c.getTecnico().getId().equals(solicitante.getId()))
            throw new RuntimeException("Apenas o técnico que assumiu o chamado pode editá-lo.");

        if (dados.containsKey("titulo")) {
            String t = (String) dados.get("titulo");
            if (t == null || t.isBlank()) throw new RuntimeException("Título é obrigatório.");
            c.setTitulo(t.trim());
        }
        if (dados.containsKey("descricao")) {
            String d = (String) dados.get("descricao");
            if (d == null || d.isBlank()) throw new RuntimeException("Descrição é obrigatória.");
            c.setDescricao(d.trim());
        }
        if (dados.containsKey("categoria")) {
            Object v = dados.get("categoria");
            if (v == null || v.toString().isBlank()) {
                c.setCategoria(null);
            } else {
                String cat = v.toString().trim();
                if (!CATEGORIAS.contains(cat))
                    throw new RuntimeException("Categoria inválida.");
                c.setCategoria(cat);
            }
        }
        if (dados.containsKey("prioridade")) {
            String p = (String) dados.get("prioridade");
            if (p != null && !PRIORIDADES.contains(p))
                throw new RuntimeException("Prioridade inválida.");
            if (p != null) c.setPrioridade(p);
        }
        if (dados.containsKey("ativoId")) {
            Object v = dados.get("ativoId");
            if (v == null || v.toString().isBlank()) {
                c.setAtivo(null);
            } else {
                Ativo a = ativoRepo.findById(Long.valueOf(v.toString()))
                        .orElseThrow(() -> new RuntimeException("Ativo não encontrado."));
                c.setAtivo(a);
            }
        }
        if (dados.containsKey("tecnicoId")) {
            Object v = dados.get("tecnicoId");
            if (v == null || v.toString().isBlank()) {
                c.setTecnico(null);
                if ("EM_ANDAMENTO".equals(c.getStatus()))
                    c.setStatus("ABERTO");
            } else {
                Usuario t = usuarioRepo.findById(Long.valueOf(v.toString()))
                        .orElseThrow(() -> new RuntimeException("Técnico não encontrado."));
                if (!"TI".equals(t.getTipoAcesso()))
                    throw new RuntimeException("Apenas usuários TI podem ser técnicos.");
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
            throw new RuntimeException("Apenas o técnico que assumiu o chamado pode alterar o status.");

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
            throw new RuntimeException("Apenas o técnico que assumiu o chamado pode vincular o ativo.");
        if (ativoId == null || ativoId.toString().isBlank()) {
            c.setAtivo(null);
        } else {
            Ativo a = ativoRepo.findById(Long.valueOf(ativoId.toString()))
                    .orElseThrow(() -> new RuntimeException("Ativo não encontrado."));
            c.setAtivo(a);
        }
        Chamado salvo = chamadoRepo.save(c);
        log.info("Ativo do chamado {} alterado", id);
        return salvo;
    }

    @Transactional
    public void excluir(Long id, Usuario solicitante) {
        Chamado c = buscarPorId(id);
        if (c.getTecnico() == null || !c.getTecnico().getId().equals(solicitante.getId()))
            throw new RuntimeException("Apenas o técnico que assumiu o chamado pode excluí-lo.");
        chamadoRepo.deleteById(id);
        log.info("Chamado {} excluído", id);
    }
}