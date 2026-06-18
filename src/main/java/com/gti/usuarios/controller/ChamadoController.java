package com.gti.usuarios.controller;

import com.gti.usuarios.model.Chamado;
import com.gti.usuarios.model.MensagemChamado;
import com.gti.usuarios.model.Usuario;
import com.gti.usuarios.repository.UsuarioRepository;
import com.gti.usuarios.service.ChamadoService;
import com.gti.usuarios.service.MensagemChamadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ChamadoController {

    private static final Logger log = LoggerFactory.getLogger(ChamadoController.class);

    private final ChamadoService chamadoService;
    private final MensagemChamadoService mensagemService;
    private final UsuarioRepository usuarioRepo;

    public ChamadoController(ChamadoService chamadoService,
                             MensagemChamadoService mensagemService,
                             UsuarioRepository usuarioRepo) {
        this.chamadoService  = chamadoService;
        this.mensagemService = mensagemService;
        this.usuarioRepo     = usuarioRepo;
    }

    private Usuario usuarioLogado() {
        String nomeUsuario = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepo.findByNomeUsuario(nomeUsuario)
                .orElseThrow(() -> new RuntimeException("Usuário do token não encontrado."));
    }

    private boolean ehTI(Usuario u) { return "TI".equals(u.getTipoAcesso()); }

    // ── Listagem ──────────────────────────────────────────────────────────────
    @GetMapping("/api/chamados")
    public ResponseEntity<?> listar() {
        try {
            Usuario u = usuarioLogado();
            List<Chamado> lista = ehTI(u)
                    ? chamadoService.listarTodos()
                    : chamadoService.listarPorSolicitante(u.getId());
            return ResponseEntity.ok(lista);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/api/chamados/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            Usuario u = usuarioLogado();
            Chamado c = chamadoService.buscarPorId(id);
            if (!ehTI(u) && !c.getSolicitante().getId().equals(u.getId()))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("erro", "Sem permissão para visualizar este chamado."));
            return ResponseEntity.ok(c);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/api/ativos/{ativoId}/chamados")
    public ResponseEntity<?> listarPorAtivo(@PathVariable Long ativoId) {
        try {
            return ResponseEntity.ok(chamadoService.listarPorAtivo(ativoId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // ── Criação (COMUM ou TI) ─────────────────────────────────────────────────
    @PostMapping("/api/chamados")
    public ResponseEntity<?> criar(@RequestBody Map<String, String> body) {
        try {
            Usuario solicitante = usuarioLogado();
            Chamado salvo = chamadoService.criar(body.get("titulo"), body.get("descricao"), solicitante);
            return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // ── Atualização (TI) ──────────────────────────────────────────────────────
    @PutMapping("/api/chamados/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Map<String, Object> dados) {
        try {
            if (!ehTI(usuarioLogado()))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("erro", "Apenas usuários TI podem editar chamados."));
            return ResponseEntity.ok(chamadoService.atualizar(id, dados, usuarioLogado()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PatchMapping("/api/chamados/{id}/assumir")
    public ResponseEntity<?> assumir(@PathVariable Long id) {
        try {
            Usuario u = usuarioLogado();
            if (!ehTI(u))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("erro", "Apenas TI pode assumir chamados."));
            return ResponseEntity.ok(chamadoService.assumir(id, u));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PatchMapping("/api/chamados/{id}/status")
    public ResponseEntity<?> alterarStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            if (!ehTI(usuarioLogado()))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("erro", "Apenas TI pode alterar status."));
            return ResponseEntity.ok(chamadoService.alterarStatus(id, body.get("status"), body.get("mensagem"), usuarioLogado()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PatchMapping("/api/chamados/{id}/reabrir")
    public ResponseEntity<?> reabrir(@PathVariable Long id) {
        try {
            Usuario u = usuarioLogado();
            if (!ehTI(u))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("erro", "Apenas TI pode reabrir chamados."));
            return ResponseEntity.ok(chamadoService.reabrir(id, u));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PatchMapping("/api/chamados/{id}/ativoId")
    public ResponseEntity<?> alterarAtivo(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Usuario u = usuarioLogado();
            if (!ehTI(u))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("erro", "Apenas TI pode vincular ativo."));
            return ResponseEntity.ok(chamadoService.alterarAtivo(id, body.get("ativoId"), u));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/api/chamados/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        try {
            if (!ehTI(usuarioLogado()))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("erro", "Apenas TI pode excluir chamados."));
            chamadoService.excluir(id, usuarioLogado());
            return ResponseEntity.ok(Map.of("mensagem", "Chamado excluído."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    // ── Mensagens (chat) ──────────────────────────────────────────────────────
    @GetMapping("/api/chamados/{id}/mensagens")
    public ResponseEntity<?> listarMensagens(@PathVariable Long id) {
        try {
            Usuario u = usuarioLogado();
            Chamado c = chamadoService.buscarPorId(id);
            if (!ehTI(u) && !c.getSolicitante().getId().equals(u.getId()))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("erro", "Sem permissão."));
            return ResponseEntity.ok(mensagemService.listar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/api/chamados/{id}/mensagens")
    public ResponseEntity<?> enviarMensagem(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            Usuario autor = usuarioLogado();
            MensagemChamado m = mensagemService.enviar(id, body.get("mensagem"), autor);
            return ResponseEntity.status(HttpStatus.CREATED).body(m);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}