package com.gti.usuarios.controller;

import com.gti.usuarios.model.Usuario;
import com.gti.usuarios.security.JwtUtil;
import com.gti.usuarios.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class UsuarioController {

    private final UsuarioService service;
    private final JwtUtil jwtUtil;
    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);


    public UsuarioController(UsuarioService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String nomeUsuario = body.get("nomeUsuario");
        String senha       = body.get("senha");
        try {
            Usuario usuario = service.login(nomeUsuario, senha);
            String token = jwtUtil.gerarToken(usuario.getId(), usuario.getNomeUsuario(), usuario.getTipoAcesso(), usuario.getPrecisaTrocarSenha());
            return ResponseEntity.ok(Map.of(
                    "token",              token,
                    "id",                 usuario.getId(),
                    "nomeUsuario",        usuario.getNomeUsuario(),
                    "nomeCompleto",       usuario.getNomeCompleto(),
                    "tipoAcesso",         usuario.getTipoAcesso(),
                    "precisaTrocarSenha", usuario.getPrecisaTrocarSenha()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/api/auth/trocar-senha")
    public ResponseEntity<?> trocarSenha(@RequestBody Map<String, String> body) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof String nomeUsuario))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("erro", "Não autenticado."));

            Usuario usuario = service.trocarSenha(nomeUsuario, body.get("novaSenha"));
            String token = jwtUtil.gerarToken(usuario.getId(), usuario.getNomeUsuario(),
                    usuario.getTipoAcesso(), usuario.getPrecisaTrocarSenha());

            return ResponseEntity.ok(Map.of(
                    "token",              token,
                    "precisaTrocarSenha", usuario.getPrecisaTrocarSenha()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/api/usuarios")
    public ResponseEntity<List<Usuario>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/api/usuarios/ativos")
    public ResponseEntity<List<Usuario>> listarAtivos() {
        return ResponseEntity.ok(service.listarAtivos());
    }

    @GetMapping("/api/usuarios/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/api/usuarios")
    public ResponseEntity<?> criar(@RequestBody Usuario usuario) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(usuario));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PutMapping("/api/usuarios/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Usuario dados) {
        try {
            return ResponseEntity.ok(service.atualizar(id, dados));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PatchMapping("/api/usuarios/{id}/bloqueio")
    public ResponseEntity<?> alterarBloqueio(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        try {
            Boolean bloqueado = body.get("bloqueado");
            if (bloqueado == null) return ResponseEntity.badRequest().body(Map.of("erro", "Campo 'bloqueado' é obrigatório."));
            return ResponseEntity.ok(service.alterarBloqueio(id, bloqueado));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/api/usuarios/{id}/impacto-exclusao")
    public ResponseEntity<?> impactoExclusao(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.impactoExclusao(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/api/usuarios/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        try {
            service.excluir(id);
            return ResponseEntity.ok(Map.of("mensagem", "Usuário excluído permanentemente."));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Falha de integridade ao excluir usuário {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("erro", "Não é possível excluir: o usuário ainda possui vínculos no sistema."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }
}