package com.gti.usuarios.controller;

import com.gti.usuarios.model.Ativo;
import com.gti.usuarios.model.ValorCampo;
import com.gti.usuarios.service.AtivoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ativos")
public class AtivoController {

    private static final Logger log = LoggerFactory.getLogger(AtivoController.class);

    private final AtivoService service;

    public AtivoController(AtivoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Ativo>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/{id}/valores")
    public ResponseEntity<?> buscarValores(@PathVariable Long id) {
        try {
            List<ValorCampo> valores = service.buscarValores(id);
            return ResponseEntity.ok(valores);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, Object> dados) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dados));
        } catch (RuntimeException e) {
            log.warn("Erro ao criar ativo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id,
                                       @RequestBody Map<String, Object> dados) {
        try {
            return ResponseEntity.ok(service.atualizar(id, dados));
        } catch (RuntimeException e) {
            log.warn("Erro ao atualizar ativo ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}