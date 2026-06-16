package com.gti.usuarios.controller;

import com.gti.usuarios.model.CampoDinamico;
import com.gti.usuarios.model.TipoAtivo;
import com.gti.usuarios.service.TipoAtivoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tipos-ativo")
public class TipoAtivoController {

    private final TipoAtivoService service;

    public TipoAtivoController(TipoAtivoService service) {
        this.service = service;
    }

    // ── Tipos ──────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<TipoAtivo>> listarTipos() {
        return ResponseEntity.ok(service.listarTipos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarTipo(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.buscarTipoPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> criarTipo(@RequestBody TipoAtivo tipo) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.criarTipo(tipo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarTipo(@PathVariable Long id, @RequestBody TipoAtivo dados) {
        try {
            return ResponseEntity.ok(service.atualizarTipo(id, dados));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirTipo(@PathVariable Long id) {
        try {
            service.excluirTipo(id);
            return ResponseEntity.ok(Map.of("mensagem", "Tipo excluído com sucesso."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    // ── Campos ─────────────────────────────────────────────
    @GetMapping("/{id}/campos")
    public ResponseEntity<?> listarCampos(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.listarCampos(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/{id}/campos")
    public ResponseEntity<?> adicionarCampo(@PathVariable Long id, @RequestBody CampoDinamico campo) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarCampo(id, campo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/campos/{campoId}")
    public ResponseEntity<?> excluirCampo(@PathVariable Long campoId) {
        try {
            service.excluirCampo(campoId);
            return ResponseEntity.ok(Map.of("mensagem", "Campo excluído com sucesso."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }
}