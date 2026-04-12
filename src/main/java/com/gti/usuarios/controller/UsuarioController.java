package com.gti.usuarios.controller;

import com.gti.usuarios.model.Usuario;
import com.gti.usuarios.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CONTROLLER — Endpoints REST
 *
 * Base URL: http://localhost:8080/api/usuarios
 *
 * GET    /api/usuarios          → lista todos
 * GET    /api/usuarios/{id}     → busca por id
 * POST   /api/usuarios          → cria novo
 * PUT    /api/usuarios/{id}     → atualiza
 * PATCH  /api/usuarios/{id}/bloqueio → bloqueia/desbloqueia
 * DELETE /api/usuarios/{id}     → exclui
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*") // permite chamadas do frontend HTML
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    // ─── GET /api/usuarios ───────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Usuario>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    // ─── GET /api/usuarios/{id} ──────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", e.getMessage()));
        }
    }

    // ─── POST /api/usuarios ──────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Usuario usuario) {
        try {
            Usuario criado = service.criar(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(criado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", e.getMessage()));
        }
    }

    // ─── PUT /api/usuarios/{id} ──────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id,
                                       @RequestBody Usuario dados) {
        try {
            return ResponseEntity.ok(service.atualizar(id, dados));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", e.getMessage()));
        }
    }

    // ─── PATCH /api/usuarios/{id}/bloqueio ───────────────────────────────────
    // Body esperado: { "bloqueado": true }
    @PatchMapping("/{id}/bloqueio")
    public ResponseEntity<?> alterarBloqueio(@PathVariable Long id,
                                             @RequestBody Map<String, Boolean> body) {
        try {
            Boolean bloqueado = body.get("bloqueado");
            if (bloqueado == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Campo 'bloqueado' é obrigatório."));
            }
            return ResponseEntity.ok(service.alterarBloqueio(id, bloqueado));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", e.getMessage()));
        }
    }

    // ─── DELETE /api/usuarios/{id} ───────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        try {
            service.excluir(id);
            return ResponseEntity.ok(Map.of("mensagem", "Usuário excluído com sucesso."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", e.getMessage()));
        }
    }
}
